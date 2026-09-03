package com.igrium.craftui.api;

import com.igrium.craftui.api.app.CraftApp;
import com.igrium.craftui.impl.AppManager;
import com.igrium.craftui.impl.input.MouseUtils;
import com.igrium.craftui.impl.util.RaycastManager;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * Global methods for controlling the lifecycle of CraftApps
 */
@UtilityClass
public final class CraftUI {
    /**
     * The draw function for a popup that renders over everything else
     */
    public static Runnable getGlobalPopup() {
        return AppManager.getGlobalPopup();
    }

    /**
     * The draw function for a popup that renders over everything else
     */
    public static void setGlobalPopup(Runnable globalPopup) {
        AppManager.setGlobalPopup(globalPopup);
    }

    /**
     * Get all the apps that are open.
     * @return An unmodifiable view of all open apps.
     */
    public static Collection<CraftApp> getApps() {
        return AppManager.getApps();
    }

    /**
     * Queue an app for opening. App will be opened at the beginning of the next render cycle.
     * @param app The app to open.
     */
    public static void openApp(@NotNull CraftApp app) {
        AppManager.openApp(app);
    }

    /**
     * Queue an app for closing. App will be closed at the beginning of the next render cycle.
     * @param app The app to close.
     */
    public static void closeApp(CraftApp app) {
        AppManager.closeApp(app);
    }

    public static boolean isOpen(CraftApp app, boolean includeQueued) {
        return AppManager.isOpen(app, includeQueued);
    }

    public static boolean isOpen(CraftApp app) {
        return isOpen(app, true);
    }

    /**
     * Return the custom viewport bounds evaluated from the currently running apps
     * @return The viewport bounds, or <code>null</code> if no app overrides the viewport bounds
     */
    public static @Nullable CraftApp.ViewportBounds getCustomViewportBounds() {
        return AppManager.getCustomViewportBounds();
    }

    /**
     * Return the location on the Minecraft viewport that a given 2D mouse position resides,
     * accounting for custom viewport bounds.
     *
     * @param globalX Mouse X position relative to the main window.
     * @param globalY Mouse Y position relative to the main window.
     * @return The equivalent position relative to the Minecraft viewport (appropriate to sending to ingame UI)
     */
    public static Vector2d getViewportMousePos(double globalX, double globalY) {
        CraftApp.ViewportBounds viewportBounds = getCustomViewportBounds();
        if (viewportBounds == null) {
            return new Vector2d(globalX, globalY);
        }

        return MouseUtils.calculateViewportMouse(Minecraft.getInstance().getWindow(), viewportBounds, globalX, globalY);
    }

    /**
     * Forward keyboard input to the game next frame, even if we have a widget focused.
     * Useful for viewport controls.
     */
    public static void forwardInputNextFrame() {
        AppManager.forwardInputNextFrame();
    }

    /**
     * Forward mouse input to the game next frame, even if we have a widget focused.
     * Useful for viewport controls.
     */
    public static void forwardMouseInputNextFrame() {
        AppManager.forwardMouseInputNextFrame();
    }

    /**
     * Force the mouse to be unlocked this frame, regardless of what Minecraft thinks.
     */
    public static void forceMouseUnlock() {
        AppManager.forceMouseUnlock();
    }

    /**
     * <p>ImGui has a limitation where, if there's a modal popup open, any additional popups will cause it to
     * close unless they're called from within the modal. CraftUI offers a way for mod systems to "inject" popups into
     * other UI paths using the "global popup".</p>
     *
     * <p>If you're rendering a modal and believe an external popup might need to be displayed (file dialog, etc,)
     * call <code>drawGlobalPopup</code> during the modal's render block.</p>
     */
    public static void drawGlobalPopup() {
        AppManager.drawGlobalPopup();
    }

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
        return RaycastManager.raycastViewport(x, y, distance, predicate, includeFluids);
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
        return RaycastManager.raycastViewport(x, y, width, height, distance, predicate, includeFluids);
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
        return RaycastManager.projectViewportGlobal(x, y, width, height, distance);
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
        return RaycastManager.projectViewport(x, y, width, height, distance, dest);
    }
}
