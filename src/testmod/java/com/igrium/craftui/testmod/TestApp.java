package com.igrium.craftui.testmod;

import com.igrium.craftui.app.DockSpaceApp;
import com.igrium.craftui.file.FileDialogs;
import com.igrium.craftui.font.Fonts;

import imgui.ImGui;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class TestApp extends DockSpaceApp {

    private final ImString imText = new ImString();

    @Override
    protected void renderApp(MinecraftClient client, int dockSpaceId) {
       setViewportInputMode(ViewportInputMode.NONE);

        ImGui.pushFont(Fonts.inter());
        if (beginViewport("Viewport", 0)) {
            queryViewportInput(ImGuiMouseButton.Right);
            ImGui.button("This is a button in the viewport!");
            ImGui.text("This is the viewport!");
        }
        ImGui.end();


        if (ImGui.begin("Upper Window")) {
            boolean clicked = ImGui.button("This is the upper window!");
            if (clicked) {
                FileDialogs.showOpenDialog(client.runDirectory.getAbsolutePath(),
                                new FileDialogs.FileFilter("Jpeg Files", ".jpg", ".jpeg"),
                                new FileDialogs.FileFilter("PNG Files", ".png"))
                        .thenAcceptAsync(opt -> {
                            if (opt.isPresent()) {
                                client.player.sendMessage(Text.literal("You chose " + opt.get()));
                            } else {
                                client.player.sendMessage(Text.literal("You didn't select a file."));
                            }
                        }, client);
            }
            ImGui.inputText("Type some text.", imText);
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
