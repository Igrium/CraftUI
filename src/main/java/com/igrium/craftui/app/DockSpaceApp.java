package com.igrium.craftui.app;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiDockNodeFlags;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiWindowFlags;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;

/**
 * A CraftApp that creates an ImGui dockspace with a game viewport in the center.
 */
public abstract class DockSpaceApp extends CraftApp {

    /**
     * Specifies the ui's behavior when the vanilla viewport is interacted with.
     */
    public enum ViewportInputMode {
        /**
         * No interaction with the vanilla game permitted.
         */
        NONE,
        /**
         * The viewport can be focused like a widget, at which point inputs are sent to the game.
         */
        FOCUS
    }

    /**
     * Specifies the ui's behavior when the vanilla viewport is interacted with.
     */
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private ViewportInputMode viewportInputMode = ViewportInputMode.FOCUS;

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
        ImGui.setNextWindowDockID(dockSpaceId);
        if (!ImGui.begin(name, imGuiWindowFlags | ImGuiWindowFlags.NoBackground)) {
            return false;
        }

        if (viewportInputMode == ViewportInputMode.FOCUS && ImGui.isWindowFocused()) {
            ImGui.setWindowFocus(null);
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
