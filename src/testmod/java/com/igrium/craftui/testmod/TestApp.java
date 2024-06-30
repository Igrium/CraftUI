package com.igrium.craftui.testmod;

import com.igrium.craftui.DockSpaceApp;
import com.igrium.craftui.util.ImFontManager;

import imgui.ImGui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class TestApp extends DockSpaceApp {

    @Override
    protected void renderApp(MinecraftClient client, int dockSpaceId) {
        // ImGui.pushFont(ImFontManager.getFont(new Identifier("craftfx:inter-medium.ttf")));
        if (beginViewport("Viewport", 0)) {
            ImGui.button("This is a button in the viewport!");
            ImGui.text("This is the viewport!");
        }
        ImGui.end();


        if (ImGui.begin("Upper Window")) {
            ImGui.button("This is the upper window!");
        }
        // ImGui.popFont();
        ImGui.end();
    }

    // @Override
    // public void render(MinecraftClient client) {
    //     // if (ImGui.begin("Main", 131502)) {
    //     //     ImGui.button("I am a button!");
    //     //     ImGui.end();
    //     // }
        
    //     ImGui.setNextWindowBgAlpha(.2f);
    //     int mainDock = ImGui.dockSpaceOverViewport(ImGui.getMainViewport(), ImGuiDockNodeFlags.NoCentralNode);

    //     ImGui.setNextWindowBgAlpha(0f);
    //     ImGui.setNextWindowDockID(mainDock);
    //     if (ImGui.begin("Main Window", ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoCollapse)) {
    //         ImGui.button("I am a button!");
    //         // ImGui.image(framebuffer.getColorAttachment(), framebuffer.textureWidth, framebuffer.textureHeight);
    //         ImGui.end();
    //     }

    //     if (ImGui.begin("Upper Window")) {
    //         ImGui.text("This is the upper window!");
    //         ImGui.end();
    //     }
    // }

    // @Override
    // protected ViewportBounds getCustomViewportBounds() {
    //     return new ViewportBounds(256, 256, 768, 768);
    // }
    
}
