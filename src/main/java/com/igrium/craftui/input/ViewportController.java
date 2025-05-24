package com.igrium.craftui.input;

import com.igrium.craftui.app.DockSpaceApp;
import imgui.ImGui;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;

/**
 * Handles viewport controls when the user clicks on the viewport. Useful to avoid having to press T to access the mouse.
 */
public class ViewportController {
    @Getter
    private final DockSpaceApp app;

    @Getter
    private boolean viewportControlsEnabled;

    /**
     * If set, the client's screen will automatically be removed when viewport controls are enabled.
     * Otherwise, viewport controls can't be enabled when a screen is open.
     */
    @Getter
    @Setter
    private boolean autoDisableScreen = false;

    /**
     * Auto-enable the viewport controls when the user presses this button.
     * (0=left, 1=middle, 2=right), -1 to disable.
     */
    @Getter
    @Setter
    private int autoEnableButton = -1;

    public ViewportController(DockSpaceApp app) {
        this.app = app;
        app.setViewportInputMode(DockSpaceApp.ViewportInputMode.NONE);
    }

    public void enableViewportControls() {
        // We can't enable viewport controls if the client has a screen.
        if (viewportControlsEnabled)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) {
            if (autoDisableScreen) {
                client.setScreen(null);
            } else {
                return; // No enabling viewport controls with a screen open.
            }
        }

        app.setViewportInputMode(DockSpaceApp.ViewportInputMode.ALWAYS);
    }

    public void disableViewportControls() {
        if (!viewportControlsEnabled)
            return;

        app.setViewportInputMode(DockSpaceApp.ViewportInputMode.NONE);
    }

    public final void setViewportControlsEnabled(boolean enabled) {
        if (enabled)
            enableViewportControls();
        else
            disableViewportControls();
    }

    /**
     * Check if the user has the mouse over the viewport and a given button is pressed.
     * @param button Mouse button to check (0=left, 1=middle, 2=right)
     */
    private static boolean isMouseDown(int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.mouse.isCursorLocked()) {
            return switch (button) {
                case 0 -> client.mouse.wasLeftButtonClicked();
                case 1 -> client.mouse.wasMiddleButtonClicked();
                case 2 -> client.mouse.wasRightButtonClicked();
                default -> false;
            };
        } else {
            return ImGui.isMouseDown(button) && ImGui.isWindowHovered();
        }
    }

    /**
     * Called after the primary viewport is rendered. Viewport must still be the current window.
     * @apiNote Will automatically be called if this is set as the app's viewport controller.
     */
    public void onRenderViewport() {
        if (autoEnableButton < 0)
            return;

        setViewportControlsEnabled(isMouseDown(autoEnableButton));
    }
}
