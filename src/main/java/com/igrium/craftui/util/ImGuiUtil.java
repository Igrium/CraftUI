package com.igrium.craftui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.craftui.CraftUI;
import com.igrium.craftui.event.ImGuiEvents;
import com.mojang.blaze3d.systems.RenderSystem;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.client.MinecraftClient;

public class ImGuiUtil {
    public static final ImGuiImplGlfw IM_GLFW = new ImGuiImplGlfw();
    public static final ImGuiImplGl3 IM_GL3 = new ImGuiImplGl3();

    private static final String GLSL_VERSION = "#version 150";

    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static final Logger LOGGER = LoggerFactory.getLogger("ImGui Integration");

    private static boolean initialized;

    public static void ensureInitialized() {
        if (initialized)
            return;
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(ImGuiUtil::ensureInitialized);
        } else {
            init();
        }
    }

    public static void init() {
        RenderSystem.assertOnRenderThread();
        if (initialized) {
            LOGGER.warn("ImGui has already been initialized!");
            return;
        }

        ImGui.createContext();
        ImGuiEvents.PRE_INIT.invoker().preInit();

        ImGui.getIO().addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        ImGui.getIO().addConfigFlags(ImGuiConfigFlags.DockingEnable);
        ImGui.getIO().setConfigMacOSXBehaviors(MinecraftClient.IS_SYSTEM_MAC);
        if (CraftUI.getConfig().isEnableViewports())
            ImGui.getIO().addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        ImGui.getIO().setIniFilename(null);
        
        ImGuiEvents.INIT_IO.invoker().initIO(ImGui.getIO());
        
        IM_GLFW.init(client.getWindow().getHandle(), true);
        IM_GL3.init(GLSL_VERSION);

        ImGuiEvents.POST_INIT.invoker().postInit();
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
