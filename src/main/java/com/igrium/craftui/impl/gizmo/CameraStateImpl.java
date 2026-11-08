package com.igrium.craftui.impl.gizmo;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.jspecify.annotations.NonNull;

@UtilityClass
public class CameraStateImpl {

    public static void register() {
        LevelRenderEvents.START_MAIN.register(context -> {
            var cameraState = context.levelState().cameraRenderState;
            projectionMatrix.set(cameraState.projectionMatrix);
            cameraPos = cameraState.pos;
            cameraRot.set(cameraState.orientation);
            tickDelta = context.levelState().worldPartialTicks;
            depthFar = cameraState.depthFar;

            // Flip depth for gizmo projection matrix because MC has it backwards apparently
            float near = 0.05f;
            float far = cameraState.depthFar;

            gizmoProjectionMatrix.m22((far + near) / (near - far))
                    .m32(2 * far * near / (near - far));

        });
    }

    @Getter @Setter
    private static @NonNull Vec3 cameraPos = Vec3.ZERO;

    @Getter
    private static final Quaternionf cameraRot = new Quaternionf();


    @Getter
    private static final Matrix4f projectionMatrix = new Matrix4f();


    @Getter
    private static final Matrix4f gizmoProjectionMatrix = new Matrix4f();

    @Getter @Setter
    private static float tickDelta;

    @Getter @Setter
    private static float depthFar;
}
