package com.igrium.craftui.app;

import com.igrium.craftui.impl.input.CursorLockManager;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiFocusedFlags;
import imgui.flag.ImGuiWindowFlags;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

/**
 * A CraftApp that creates an ImGui dockspace with a game viewport in the center.
 * Only one of these should be active at a time.
 */
public abstract class DockSpaceApp extends CraftApp {

    /**
     * Specifies the ui's behavior when the vanilla viewport is interacted with.
     */
    public enum ViewportInputMode {
        /**
         * Never forward input to Minecraft and keep the mouse unlocked.
         */
        NONE,
        /**
         * Forward input to Minecraft whenever the mouse is pressed over the viewport
         * @see #viewportInputButtons
         */
        HOLD,
        /**
         * Forward input to Minecraft if the viewport window is focused.
         */
        FOCUS,
        /**
         * If the vanilla client wants to lock the cursor, always do it.
         */
        ALWAYS
    }

    @Getter
    private int dockSpaceId;

    private ViewportBounds viewportBounds = new ViewportBounds(0, 0, 1, 1);

    private boolean didBeginViewport;

    /**
     * The viewport input mode to use on this frame. Default: <code>ViewportInputMode.FOCUS</code>
     */
    @Getter @Setter
    private ViewportInputMode viewportInputMode = ViewportInputMode.FOCUS;

    /**
     * If the input mode is set to <code>ViewportInputMode.HOLD</code>,
     * forward viewport input whenever one of these buttons is pressed.
     * @implNote Currently does not work with left mouse button
     */
    @Getter
    private int @Nullable [] viewportInputButtons = new int[] { 1 };

    public void setViewportInputButtons(int... buttons) {
        this.viewportInputButtons = buttons;
    }

    @Override
    protected void render(MinecraftClient client) {
        ImGui.setNextWindowBgAlpha(0f);
        dockSpaceId = ImGui.dockSpaceOverViewport(ImGui.getMainViewport().getID());
        didBeginViewport = false;
    }

    /**
     * Check if the mouse is currently pressed over the game viewport.
     * @param mouseButton ImGui mouse button to check. (0=left, 1=middle, 2=right)
     */
    // TODO: Find a better place to put this
    protected static boolean mousePressedOverViewport(int mouseButton) {
        return (ImGui.isWindowHovered() || MinecraftClient.getInstance().mouse.isCursorLocked()) && ImGui.isMouseDown(mouseButton);
    }

    /**
     * If any window is focused, unlock the mouse
     */
    private static void unlockIfWindowFocused() {
        if (ImGui.isWindowFocused(ImGuiFocusedFlags.AnyWindow)) {
            AppManager.forceMouseUnlock();
        }
    }

    private boolean isViewportButtonDown() {
        if (viewportInputButtons == null)
            return false;

        for (var button : viewportInputButtons) {
            if (mousePressedOverViewport(button))
                return true;
        }
        return false;
    }

    protected final boolean beginViewport(String name, int imGuiWindowFlags) {
        if (didBeginViewport) {
            throw new IllegalStateException("beginViewport only may be called once per frame! Also, make sure you're calling super.render() at the beginning of render.");
        }
        didBeginViewport = true;

        ImGui.setNextWindowDockID(dockSpaceId);
        if (!ImGui.begin(name, imGuiWindowFlags | ImGuiWindowFlags.NoBackground)) {
            return false;
        }

        // Handle mouse locking. Technically this doesn't have to do with the viewport, but this place is convenient.

        if (viewportInputMode == ViewportInputMode.FOCUS || !CursorLockManager.clientWantsLockCursor()) {
            // If client cursor is unlocked, always use focus mode so vanilla text fields work
            if (ImGui.isWindowFocused()) {
                ImGui.setWindowFocus(null);
            }
            unlockIfWindowFocused();
        } else {
            switch (viewportInputMode) {
                case NONE -> {
                    if (!ImGui.isWindowFocused(ImGuiFocusedFlags.AnyWindow)) {
                        ImGui.setWindowFocus();
                    }
                    AppManager.forceMouseUnlock();
                }
                case HOLD -> {
                    if (isViewportButtonDown()) {
                        ImGui.setWindowFocus(null);
                    } else if (!ImGui.isWindowFocused(ImGuiFocusedFlags.AnyWindow)) {
                        ImGui.setWindowFocus();
                    }
                    unlockIfWindowFocused();
                }
                case ALWAYS -> {
                    ImGui.setWindowFocus(null);
                    MinecraftClient.getInstance().mouse.lockCursor();
                }
            }
        }



        // Force mouse inputs to be forwarded to screen if there is any.
        if (ImGui.isWindowHovered() && MinecraftClient.getInstance().currentScreen != null) {
            AppManager.forwardMouseInputNextFrame();
        }

        float minX = ImGui.getWindowContentRegionMinX();
        float maxX = ImGui.getWindowContentRegionMaxX();
        float minY = ImGui.getWindowContentRegionMinY();
        float maxY = ImGui.getWindowContentRegionMaxY();

        float xPos;
        float yPos;

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            xPos = ImGui.getWindowPosX() - ImGui.getWindowViewport().getPosX() + minX;
            yPos = ImGui.getWindowPosY() - ImGui.getWindowViewport().getPosY() + minY;
        } else {
            xPos = ImGui.getWindowPosX() + minX;
            yPos = ImGui.getWindowPosY() + minY;
        }

        // TODO: Can I deal with the OpenGL flipped y bullshittary better?
        yPos = ImGui.getWindowViewport().getSizeY() - yPos - (maxY - minY);

        float width = Math.max(maxX - minX, 1);
        float height = Math.max(maxY - minY, 1);

        viewportBounds = new ViewportBounds((int) xPos, (int) yPos, (int) width, (int) height);

        return true;
    }

    @Override
    protected ViewportBounds getCustomViewportBounds() {
        return viewportBounds;
    }
}
