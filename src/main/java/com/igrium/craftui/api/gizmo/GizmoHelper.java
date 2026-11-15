package com.igrium.craftui.api.gizmo;

import com.igrium.craftui.api.CraftUI;
import com.igrium.craftui.api.app.CraftApp.ViewportBounds;
import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.extension.imguizmo.ImGuizmo;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@UtilityClass
public final class GizmoHelper {

    /**
     * Configures ImGuizmo to draw properly inside the viewport window (projection, etc.).
     * Call inside the viewport ImGui window, before attempting to draw any gizmos
     *
     * @implSpec Uses the viewport bounds returned by {@link CraftUI#getCustomViewportBounds()}
     */
    public static void setupGizmos() {
        setupGizmos(CraftUI.getCustomViewportBounds());
    }

    /**
     * Configures ImGuizmo to draw properly inside the viewport window (projection, etc.).
     * Call inside the viewport ImGui window, before attempting to draw any gizmos
     *
     * @param viewportBounds The custom viewport bounds to use, if any
     */
    public static void setupGizmos(@Nullable ViewportBounds viewportBounds) {
        Minecraft mc = Minecraft.getInstance();

        ImGuizmo.setDrawList();
        int screenWidth = mc.getWindow().getScreenWidth();
        int screenHeight = mc.getWindow().getScreenHeight();

        float rectX = viewportBounds != null ? viewportBounds.x() : 0;
        float rectY = viewportBounds != null
                ? screenHeight - viewportBounds.y() - viewportBounds.height()
                : 0;

        ImGuiViewport vp = ImGui.getMainViewport();
        rectX += vp.getPosX();
        rectY += vp.getPosY();

        float rectWidth = viewportBounds != null ? viewportBounds.width() : screenWidth;
        float rectHeight = viewportBounds != null ? viewportBounds.height() : screenHeight;

        ImGuizmo.setRect(rectX, rectY, rectWidth, rectHeight);
    }

    private static final float[] viewMatrixBuffer = new float[16];
    private static final float[] projectionMatrixBuffer = new float[16];
    private static final float[] modelMatrixBuffer = new float[16];
    private static final float[] deltaMatrixBuffer = new float[16];

    /**
     * Wrapper over {@link ImGuizmo#manipulate} which uses joml matrices
     *
     * @param viewMatrix       target camera view
     * @param projectionMatrix target camera projection
     * @param operation        target operation
     * @param mode             target mode
     * @param modelMatrix      model matrix
     * @param deltaMatrix      delta matrix
     * @param snap             snap value
     */
    public static void manipulate(Matrix4fc viewMatrix, Matrix4fc projectionMatrix, int operation, int mode,
                                  Matrix4f modelMatrix, @Nullable Matrix4f deltaMatrix, float @Nullable [] snap) {
        viewMatrix.get(viewMatrixBuffer);
        projectionMatrix.get(projectionMatrixBuffer);
        modelMatrix.get(modelMatrixBuffer);
        if (deltaMatrix != null) deltaMatrix.get(deltaMatrixBuffer);

        ImGuizmo.manipulate(viewMatrixBuffer, projectionMatrixBuffer, operation, mode,
                modelMatrixBuffer, deltaMatrixBuffer, snap);

        modelMatrix.set(modelMatrixBuffer);
        if (deltaMatrix != null) deltaMatrix.set(deltaMatrixBuffer);
    }

    /**
     * Wrapper over {@link ImGuizmo#manipulate} which uses joml matrices
     *
     * @param viewMatrix       target camera view
     * @param projectionMatrix target camera projection
     * @param operation        target operation
     * @param mode             target mode
     * @param modelMatrix      model matrix
     * @param deltaMatrix      delta matrix
     */
    public static void manipulate(Matrix4fc viewMatrix, Matrix4fc projectionMatrix, int operation, int mode,
                                  Matrix4f modelMatrix, @Nullable Matrix4f deltaMatrix) {
        manipulate(viewMatrix, projectionMatrix, operation, mode, modelMatrix, deltaMatrix, null);
    }

    /**
     * Wrapper over {@link ImGuizmo#manipulate} which uses joml matrices
     *
     * @param viewMatrix       target camera view
     * @param projectionMatrix target camera projection
     * @param operation        target operation
     * @param mode             target mode
     * @param modelMatrix      model matrix
     */
    public static void manipulate(Matrix4fc viewMatrix, Matrix4fc projectionMatrix, int operation, int mode,
                                  Matrix4f modelMatrix) {
        manipulate(viewMatrix, projectionMatrix, operation, mode, modelMatrix, null);
    }

    /**
     * Call {@link #manipulate} using the cached Minecraft camera transform.
     *
     * @param operation   target operation
     * @param mode        target mode
     * @param modelMatrix model matrix
     * @param deltaMatrix delta matrix
     * @param snap        snap value
     * @apiNote Minecraft represents its model matrices <em>relative to the camera</em>
     */
    public static void manipulateView(int operation, int mode, Matrix4f modelMatrix,
                                      @Nullable Matrix4f deltaMatrix, float @Nullable [] snap) {
        manipulate(CraftUI.getViewRotationMatrix(), CraftUI.getProjectionMatrix(), operation, mode, modelMatrix, deltaMatrix,
                snap);
    }

    /**
     * Call {@link #manipulate} using the cached Minecraft camera transform.
     *
     * @param operation   target operation
     * @param mode        target mode
     * @param modelMatrix model matrix
     * @param deltaMatrix delta matrix
     * @apiNote Minecraft represents its model matrices <em>relative to the camera</em>
     */
    public static void manipulateView(int operation, int mode, Matrix4f modelMatrix, @Nullable Matrix4f deltaMatrix) {
        manipulateView(operation, mode, modelMatrix, deltaMatrix, null);
    }

    /**
     * Call {@link #manipulate} using the cached Minecraft camera transform.
     *
     * @param operation   target operation
     * @param mode        target mode
     * @param modelMatrix model matrix
     * @apiNote Minecraft represents its model matrices <em>relative to the camera</em>
     */
    public static void manipulateView(int operation, int mode, Matrix4f modelMatrix) {
        manipulateView(operation, mode, modelMatrix, null);
    }
}
