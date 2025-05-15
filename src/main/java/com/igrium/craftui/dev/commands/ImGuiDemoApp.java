package com.igrium.craftui.dev.commands;

import com.igrium.craftui.app.CraftApp;

import imgui.ImGui;
import net.minecraft.client.MinecraftClient;

public class ImGuiDemoApp extends CraftApp {

    public ImGuiDemoApp(String id) {
        super(id);
    }

    @Override
    protected void render(MinecraftClient client) {
        ImGui.showDemoWindow();
    }
    
}
