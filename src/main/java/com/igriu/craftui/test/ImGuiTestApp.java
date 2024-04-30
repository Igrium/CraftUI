package com.igriu.craftui.test;

import com.igriu.craftui.ImGuiUtil;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;

import static org.lwjgl.glfw.GLFW.*;

public class ImGuiTestApp {

    private static boolean isOpen;

    public static boolean isOpen() {
        return isOpen;
    }

    public static void open() {
        if (isOpen) return;

        if (!ImGuiUtil.isInitialized()) {
            ImGuiUtil.init();
        }

        isOpen = true;
    }

    public static void render() {
        if (!isOpen)
            return;
        
        ImGuiUtil.imGLFW.newFrame();
        ImGui.newFrame();

        ImGui.begin("Cool Window");
        
        ImGui.button("I am a button!");
        ImGui.end();

        ImGui.render();
        ImGuiUtil.imGL3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            long backupWindowPtr = glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            glfwMakeContextCurrent(backupWindowPtr);
        }
    }
    
    public static void close() {
        isOpen = false;
    }
}
