package com.igrium.craftui.app;

import com.igrium.craftui.app.CraftApp.ViewportBounds;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
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
        dockSpaceId = ImGui.dockSpaceOverViewport(ImGui.getMainViewport());
        renderApp(client, dockSpaceId);
    }
    
    protected abstract void renderApp(MinecraftClient client, int dockSpaceId);

    protected final boolean beginViewport(String name, int imGuiWindowFlags) {
        ImGui.setNextWindowDockID(dockSpaceId);
        if (!ImGui.begin(name, imGuiWindowFlags | ImGuiWindowFlags.NoBackground)) {
            return false;
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

        // int viewportX = (int) ImGui.getWindowPosX();
        // int viewportY = (int) ImGui.getWindowPosY();
        // int viewportWidth = (int) ImGui.getWindowWidth();
        // int viewportHeight = (int) ImGui.getWindowHeight();

        // viewportBounds = new ViewportBounds(viewportX, viewportY, viewportWidth, viewportHeight);

        return true;
    }

    @Override
    protected ViewportBounds getCustomViewportBounds() {
        return viewportBounds;
    }
}
