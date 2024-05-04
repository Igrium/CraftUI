package com.igrium.craftui.testmod;

import com.igrium.craftui.CraftApp;

import imgui.ImGui;
import net.minecraft.client.MinecraftClient;

public class TestApp extends CraftApp {

    @Override
    public void render(MinecraftClient client) {
        if (ImGui.begin("Main", 131502)) {
            ImGui.button("I am a button!");
            ImGui.end();
        }

    }

    @Override
    protected ViewportBounds getCustomViewportBounds() {
        return new ViewportBounds(256, 256, 512, 512);
    }
    
}
