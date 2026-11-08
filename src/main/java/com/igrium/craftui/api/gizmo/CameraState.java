package com.igrium.craftui.api.gizmo;

import com.igrium.craftui.impl.gizmo.CameraStateImpl;
import lombok.experimental.UtilityClass;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.jspecify.annotations.NonNull;

/**
 * Caches various attributes about game renderer so they can be accessed in ImGui
 */
@UtilityClass
public final class CameraState {
    /**
     * The absolute position of the camera in world-space
     */
    public @NonNull Vec3 getCameraPos() {
        return CameraStateImpl.getCameraPos();
    }

    /**
     * The orientation of the camera expressed as a quaternion
     */
    public Quaternionfc getCameraRot() {
        return CameraStateImpl.getCameraRot();
    }

    /**
     * Minecraft's native view-model transform
     */
    public Matrix4fc getProjectionMatrix() {
        return CameraStateImpl.getProjectionMatrix();
    }

    /**
     * A variation if the projection matrix which is inverted back to "proper" coordinates
     * for use with ImGuizmo
     */
    public Matrix4fc getGizmoProjectionMatrix() {
        return CameraStateImpl.getGizmoProjectionMatrix();
    }

    /**
     * Minecraft's "partial ticks" (or "tick delta" as per Yarn names)
     */
    public float getTickDelta() {
        return CameraStateImpl.getTickDelta();
    }
}
