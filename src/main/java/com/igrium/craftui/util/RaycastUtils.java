package com.igrium.craftui.util;


import java.util.function.Predicate;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.Window;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

/**
 * A set of utility functions involving raycasting.
 */
public final class RaycastUtils {
    private RaycastUtils() {};

    /**
     * Some functions need to register some fabric listeners. Do that here.
     */
    public static void register() {
        WorldRenderEvents.AFTER_SETUP.register(context -> {
            lastProjectionMatrix.set(context.projectionMatrix());
            lastCamera = context.camera();
        });
    }

    private static final Matrix4f lastProjectionMatrix = new Matrix4f();
    @Nullable
    private static Camera lastCamera;

    public static HitResult raycastViewport(float x, float y, float distance, Predicate<Entity> predicate, boolean includeFluids) {
        Window window = MinecraftClient.getInstance().getWindow();
        return raycastViewport(x, y, window.getWidth(), window.getHeight(), distance, predicate, includeFluids);
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
        Entity cameraEntity = MinecraftClient.getInstance().getCameraEntity();
        if (cameraEntity == null) {
            throw new IllegalStateException("No camera entity");
        }

        if (lastCamera == null) {
            throw new IllegalStateException("No frame has been rendered yet.");
        }

        Vec3d start = lastCamera.getPos();
        Vec3d end = projectViewportGlobal(x, y, width, height, distance);

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
    public static Vec3d projectViewportGlobal(float x, float y, float width, float height, float distance) {
        if (lastCamera == null) {
            throw new IllegalStateException("No frame has been rendered yet.");
        }

        Vector3f localSpace = projectViewport(x, y, width, height, distance, new Vector3f());

        // Add components while constructing the Vec3d to avoid precision error casting to float for Vector3f
        return new Vec3d(localSpace.x + lastCamera.getPos().x,
                localSpace.y + lastCamera.getPos().y,
                localSpace.z + lastCamera.getPos().z);
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
        if (lastCamera == null) {
            throw new IllegalStateException("No frame has been rendered yet.");
        }

        Vector4f screenspace = new Vector4f(2 * x / width - 1, 2 * y / height - 1, -1, 1);

        Matrix4f cameraProjection = new Matrix4f(lastProjectionMatrix);
        cameraProjection.invert();

        cameraProjection.transform(screenspace);
        screenspace.mul(distance);
        screenspace.rotate(lastCamera.getRotation());

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
    private static HitResult raycast(Entity sourceEntity, Vec3d start, Vec3d end, Predicate<Entity> predicate, boolean includeFluids) {
        // TODO: Is it worth putting this in the public API?
        double distance = start.distanceTo(end);
        Box box = Box.of(start, 1, 1, 1)
                .stretch(sourceEntity.getRotationVec(1).multiply(distance))
                .expand(1, 1, 1);

        BlockHitResult worldHit = sourceEntity.getWorld().raycast(new RaycastContext(
                start,
                end,
                ShapeType.OUTLINE,
                includeFluids ? FluidHandling.ANY : FluidHandling.NONE,
                sourceEntity));

        EntityHitResult entityHit = ProjectileUtil.raycast(sourceEntity, start, end, box, predicate, distance);

        if (entityHit != null) {
            double entityDist = start.squaredDistanceTo(entityHit.getPos());
            double worldDist = start.squaredDistanceTo(worldHit.getPos());

            if (entityDist < worldDist) {
                return entityHit;
            }
        }

        return worldHit;
    }
}