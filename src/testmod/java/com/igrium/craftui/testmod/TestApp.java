package com.igrium.craftui.testmod;

import com.igrium.craftui.CraftApp;

import imgui.ImGui;
import net.minecraft.client.gl.Framebuffer;

public class TestApp extends CraftApp {

    @Override
    public void render(Framebuffer framebuffer) {
        if (ImGui.begin("Main", 131502)) {
            ImGui.button("I am a button!");
            ImGui.end();
        }

    }
    
}
