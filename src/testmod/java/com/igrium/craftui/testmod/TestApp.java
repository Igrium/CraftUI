package com.igrium.craftui.testmod;

import com.igrium.craftui.DockSpaceApp;
import com.igrium.craftui.file.FileDialogs;
import com.igrium.craftui.font.Fonts;
import com.igrium.craftui.font.ImFontManager;

import imgui.ImGui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class TestApp extends DockSpaceApp {

    @Override
    protected void renderApp(MinecraftClient client, int dockSpaceId) {
        ImGui.pushFont(Fonts.inter());
        if (beginViewport("Viewport", 0)) {
            ImGui.button("This is a button in the viewport!");
            ImGui.text("This is the viewport!");
        }
        ImGui.end();


        if (ImGui.begin("Upper Window")) {
            boolean clicked = ImGui.button("This is the upper window!");
            if (clicked) {
                FileDialogs.pickFolder(client.runDirectory.getAbsolutePath()).thenAcceptAsync(opt -> {
                    if (opt.isPresent()) {
                        client.player.sendMessage(Text.literal("You chose " + opt.get()));
                    } else {
                        client.player.sendMessage(Text.literal("You didn't select a file."));
                    }
                }, client);
            }
        }
        ImGui.end();
        ImGui.popFont();
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
