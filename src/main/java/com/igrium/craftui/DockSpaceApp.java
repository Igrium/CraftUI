package com.igrium.craftui;

import imgui.ImGui;
import imgui.flag.ImGuiDockNodeFlags;
import imgui.flag.ImGuiWindowFlags;
import net.minecraft.client.MinecraftClient;

/**
 * A CraftApp that creates an ImGui dockspace with a game viewport in the center.
 */
public abstract class DockSpaceApp extends CraftApp {

    private int dockSpaceId;

    private ViewportBounds viewportBounds = new ViewportBounds(0, 0, 1, 1);

    @Override
    protected void render(MinecraftClient client) {
        ImGui.setNextWindowBgAlpha(0f);
        dockSpaceId = ImGui.dockSpaceOverViewport(ImGui.getMainViewport(), ImGuiDockNodeFlags.NoCentralNode);
        renderApp(client, dockSpaceId);
    }
    
    protected abstract void renderApp(MinecraftClient client, int dockSpaceId);

    protected final boolean beginViewport(String name, int imGuiWindowFlags) {
        // ImGui.setNextWindowDockID(dockSpaceId);
        if (!ImGui.begin(name, imGuiWindowFlags | ImGuiWindowFlags.NoBackground)) {
            return false;
        }

        int viewportX = (int) ImGui.getWindowPosX();
        int viewportY = (int) ImGui.getWindowPosY();
        int viewportWidth = (int) ImGui.getWindowWidth();
        int viewportHeight = (int) ImGui.getWindowHeight();

        viewportBounds = new ViewportBounds(viewportX, viewportY, viewportWidth, viewportHeight);

        return true;
    }

    @Override
    protected ViewportBounds getCustomViewportBounds() {
        return viewportBounds;
    }
}
