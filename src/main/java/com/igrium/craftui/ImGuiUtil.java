package com.igrium.craftui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.systems.RenderSystem;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.MinecraftClient;

public class ImGuiUtil {
    public static final ImGuiImplGlfw imGLFW = new ImGuiImplGlfw();
    public static final ImGuiImplGl3 imGL3 = new ImGuiImplGl3();

    private static final String GLSL_VERSION = "#version 150";

    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static final Logger LOGGER = LoggerFactory.getLogger("ImGui Integration");

    private static boolean initialized;

    public static void init() {
        RenderSystem.assertOnRenderThread();
        if (initialized) {
            LOGGER.warn("ImGui has already been initialized!");
            return;
        }

        ImGui.createContext();
        ImGui.getIO().addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);
        ImGui.getIO().setConfigMacOSXBehaviors(MinecraftClient.IS_SYSTEM_MAC);

        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        
        imGLFW.init(client.getWindow().getHandle(), true);
        imGL3.init(GLSL_VERSION);
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
