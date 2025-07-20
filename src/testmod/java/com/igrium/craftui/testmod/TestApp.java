package com.igrium.craftui.testmod;

import com.igrium.craftui.app.AppManager;
import com.igrium.craftui.app.DockSpaceApp;
import com.igrium.craftui.file.FileDialogs;
import com.igrium.craftui.font.Fonts;

import com.igrium.craftui.input.MouseUtils;
import com.igrium.craftui.input.ViewportController;
import imgui.ImGui;
import imgui.flag.ImGuiMouseButton;
import imgui.type.ImInt;
import imgui.type.ImString;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class TestApp extends DockSpaceApp {

    private final ImString imText = new ImString();
    private final ImInt inputMode = new ImInt(1);

    private static final String[] INPUT_MODE_OPTIONS = new String[]{"None", "Focus", "Always"};

    @Override
    protected void onOpen() {
        super.onOpen();
        setViewportInputMode(ViewportInputMode.NONE);
    }


    protected void render(MinecraftClient client) {
        super.render(client);
        ImGui.pushFont(Fonts.inter());

        if (ImGui.begin("Upper Window")) {
            boolean clicked = ImGui.button("This is the upper window!");
            if (clicked) {
                FileDialogs.showOpenDialog(client.runDirectory.getAbsolutePath(),
                                new FileDialogs.FileFilter("Jpeg Files", ".jpg", ".jpeg"),
                                new FileDialogs.FileFilter("PNG Files", ".png"))
                        .thenAcceptAsync(opt -> {
                            if (opt.isPresent()) {
                                client.player.sendMessage(Text.literal("You chose " + opt.get()), false);
                            } else {
                                client.player.sendMessage(Text.literal("You didn't select a file."), false);
                            }
                        }, client);
            }
            ImGui.inputText("Type some text.", imText);

            boolean closeClicked = ImGui.button("Close App");
            if (closeClicked) {
                close();
            }

            if (ImGui.button("Close Chat Window")) {
                MinecraftClient.getInstance().setScreen(null);
            }

            ImGui.combo("Viewport Input Mode", inputMode, INPUT_MODE_OPTIONS);
//            setViewportInputMode(ViewportInputMode.values()[inputMode.get()]);
        }
        ImGui.end();

        if (beginViewport("Viewport", 0)) {
            ImGui.button("This is a button in the viewport!");
            ImGui.text("This is the viewport!");
            boolean mousePressed = mousePressedOverViewport(0);
            ImGui.text("Mouse down: " + mousePressed);
            setViewportInputMode(mousePressed ? ViewportInputMode.ALWAYS : ViewportInputMode.NONE);
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
