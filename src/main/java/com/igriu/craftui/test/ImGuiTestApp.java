package com.igriu.craftui.test;

import imgui.ImGui;
import imgui.app.Application;
import imgui.app.Configuration;

public class ImGuiTestApp extends Application {

    @Override
    protected void configure(Configuration config) {
        config.setTitle("This is a test app.");
    }

    @Override
    public void process() {
        ImGui.text("Hello world!");
    }
    
    
}
