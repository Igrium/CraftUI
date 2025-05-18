package com.igrium.craftui.app;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiFocusedFlags;
import imgui.flag.ImGuiWindowFlags;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;

/**
 * A CraftApp that creates an ImGui dockspace with a game viewport in the center.
 * Only one of these should be active at a time.
 */
public abstract class DockSpaceApp extends CraftApp {

    /**
     * Specifies the ui's behavior when the vanilla viewport is interacted with.
     */
    public static enum ViewportInputMode {
        /**
         * Never forward input to Minecraft and keep the mouse unlocked.
         */
        NONE,
        /**
         * Forward input to Minecraft if the viewport window is focused.
         */
        FOCUS,
        /**
         * Always forward input to Minecraft, even if imgui consumes it. Use with caution.
         */
        ALWAYS
    }

    @Getter
    private int dockSpaceId;

    private ViewportBounds viewportBounds = new ViewportBounds(0, 0, 1, 1);


    @Override
    protected void render(MinecraftClient client) {
        ImGui.setNextWindowBgAlpha(0f);
        dockSpaceId = ImGui.dockSpaceOverViewport(ImGui.getMainViewport());
        renderApp(client, dockSpaceId);
    }
    
    protected abstract void renderApp(MinecraftClient client, int dockSpaceId);

    protected final boolean beginViewport(String name, int imGuiWindowFlags) {
        return beginViewport(name, imGuiWindowFlags, ViewportInputMode.FOCUS);
    }

    protected final boolean beginViewport(String name, int imGuiWindowFlags, ViewportInputMode viewportInputMode) {
        ImGui.setNextWindowDockID(dockSpaceId);
        if (!ImGui.begin(name, imGuiWindowFlags | ImGuiWindowFlags.NoBackground)) {
            return false;
        }


        // Focus game when viewport is clicked.
        if (viewportInputMode == ViewportInputMode.FOCUS && ImGui.isWindowFocused()) {
            ImGui.setWindowFocus(null);
        }

        // Handle mouse locking. Technically this doesn't have to do with the viewport, but this place is convenient.
        switch(viewportInputMode) {
            case NONE -> {
                AppManager.forceMouseUnlock();
            }
            case FOCUS -> {
                if (ImGui.isWindowFocused(ImGuiFocusedFlags.AnyWindow) && !ImGui.isWindowFocused()) {
                    AppManager.forceMouseUnlock();
                }
            }
//            case ALWAYS -> {
//                AppManager.forwardInputNextFrame();
//            }
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

    /**
     * If the mouse was pressed on the most-recently rendered window, forward input to the game next frame.
     * Most likely used on the viewport.
     * @param mouseButton Mouse button to query.
     */
    protected static void queryViewportInput(int mouseButton) {
        if (ImGui.isWindowFocused() && ImGui.isMouseDown(mouseButton)) {
            AppManager.forwardInputNextFrame();
        }
    }

    @Override
    protected ViewportBounds getCustomViewportBounds() {
        return viewportBounds;
    }
}
