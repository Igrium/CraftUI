package com.igrium.craftui.impl.commands;

import com.igrium.craftui.api.app.CraftApp;

import imgui.ImGui;
import net.minecraft.client.Minecraft;

public class ImGuiDemoApp extends CraftApp {

    @Override
    public void render(Minecraft client) {
        ImGui.showDemoWindow();
    }
    
}
