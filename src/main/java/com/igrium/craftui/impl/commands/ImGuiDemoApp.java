package com.igrium.craftui.impl.commands;

import com.igrium.craftui.app.CraftApp;

import imgui.ImGui;
import net.minecraft.client.Minecraft;

public class ImGuiDemoApp extends CraftApp {

    @Override
    protected void render(Minecraft client) {
        ImGui.showDemoWindow();
    }
    
}
