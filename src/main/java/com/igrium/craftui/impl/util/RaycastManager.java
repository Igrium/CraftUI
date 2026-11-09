package com.igrium.craftui.impl.util;


import com.igrium.craftui.api.gizmo.GizmoHelper;
import com.mojang.blaze3d.platform.Window;
import java.util.function.Predicate;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * A set of utility functions involving raycasting.
 */
@UtilityClass
public final class RaycastManager {

    /**
     * Perform a raycast based on a specific point in screenspace
     *
     * @param x             Screenspace X.
     * @param y             Screenspace Y.
     * @param distance      Max distance of the raycast.
     * @param predicate     A filter of which entities are allowed for this raycast.
     * @param includeFluids Whether to include fluids
     * @return The hit result.
     */
    public static HitResult raycastViewport(float x, float y, float distance, Predicate<Entity> predicate, boolean includeFluids) {
        Window window = Minecraft.getInstance().getWindow();
        return raycastViewport(x, y, window.getScreenWidth(), window.getScreenHeight(), distance, predicate, includeFluids);
    }

    /**
     * Perform a raycast based on a specific point in screenspace.
     *
     * @param x             Screenspace X.
     * @param y             Screenspace Y.
     * @param width         Width of the viewport.
     * @param height        Height of the viewport.
     * @param distance      Max distance of the raycast.
     * @param predicate     A filter of which entities are allowed for this raycast.
     * @param includeFluids Whether to include fluids or not.
     * @return The hit result.
     */
    public static HitResult raycastViewport(float x, float y, float width, float height, float distance,
                                            Predicate<Entity> predicate, boolean includeFluids) {
        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity == null) {
            throw new IllegalStateException("No camera entity");
        }

        Vec3 start = GizmoHelper.getCameraPos();
        Vec3 end = projectViewportGlobal(x, y, width, height, distance);

        return raycast(cameraEntity, start, end, predicate, includeFluids);
    }

    /**
     * Determine the world location of a specific point in
     * screenspace.
     *
     * @param x        Screenspace X
     * @param y        Screenspace Y
     * @param width    Width of the viewport.
     * @param height   Height of the viewport.
     * @param distance Distance from the camera to place the point.
     * @return A point in 3D space that falls under the 2D screenspace point.
     */
    public static Vec3 projectViewportGlobal(float x, float y, float width, float height, float distance) {

        Vector3f localSpace = projectViewport(x, y, width, height, distance, new Vector3f());

        // Add components while constructing the Vec3d to avoid precision error casting to float for Vector3f
        var lastCameraPos = GizmoHelper.getCameraPos();
        return new Vec3(localSpace.x + lastCameraPos.x,
                localSpace.y + lastCameraPos.y,
                localSpace.z + lastCameraPos.z);
    }

    /**
     * Determine the 3d location of a specific point in screenspace.
     *
     * @param x        Screenspace X
     * @param y        Screenspace Y
     * @param width    Width of the viewport
     * @param height   Height of the viewport.
     * @param distance Distance from the camera to place the point.
     * @param dest     Vector3f to store result in.
     * @return <code>dest</code>: The 3D point <em>relative to the camera's location</em>
     */
    public static Vector3f projectViewport(float x, float y, float width, float height, float distance, Vector3f dest) {

        Vector4f screenspace = new Vector4f(2 * x / width - 1, 2 * y / height - 1, -1, 1);

        // WHY does OpenGL flip Y...
        screenspace.y = -screenspace.y;

        Matrix4f cameraProjection = new Matrix4f(GizmoHelper.getRawProjectionMatrix());
        cameraProjection.invert();

        cameraProjection.transform(screenspace);
        screenspace.mul(distance);
        screenspace.rotate(GizmoHelper.getCameraRot());

        dest.set(screenspace);
        return dest;
    }

    /**
     * Perform a simple raycast on the block world and its entities.
     *
     * @param sourceEntity  Source entity to pass to Minecraft's raycasting
     *                      functions.
     * @param start         Raycast start point.
     * @param end           Raycast end point.
     * @param predicate     A filter of which entities are allowed for this raycast.
     * @param includeFluids Whether to include fluids or not.
     * @return The hit result.
     */
    private static HitResult raycast(Entity sourceEntity, Vec3 start, Vec3 end, Predicate<Entity> predicate, boolean includeFluids) {
        double distance = start.distanceTo(end);
        AABB box = AABB.ofSize(start, 1, 1, 1)
                .expandTowards(sourceEntity.getViewVector(1).scale(distance))
                .inflate(1, 1, 1);

        BlockHitResult worldHit = sourceEntity.level().clip(new ClipContext(
                start,
                end,
                Block.OUTLINE,
                includeFluids ? Fluid.ANY : Fluid.NONE,
                sourceEntity));

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(sourceEntity, start, end, box, predicate, distance);

        if (entityHit != null) {
            double entityDist = start.distanceToSqr(entityHit.getLocation());
            double worldDist = start.distanceToSqr(worldHit.getLocation());

            if (entityDist < worldDist) {
                return entityHit;
            }
        }

        return worldHit;
    }
}