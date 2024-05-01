package com.igrium.craftui.testmod;

import com.igrium.craftui.CraftApp;

import imgui.ImGui;

public class TestApp extends CraftApp {

    @Override
    public void render() {
        ImGui.begin("Cool Window");
        ImGui.button("I am a button!");
        ImGui.end();
    }
    
}
