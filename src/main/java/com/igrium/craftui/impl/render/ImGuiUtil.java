package com.igrium.craftui.impl.render;

import imgui.extension.implot.ImPlot;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.craftui.CraftUI;
import com.igrium.craftui.event.ImGuiEvents;
import com.mojang.blaze3d.systems.RenderSystem;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;

public class ImGuiUtil {
    public static final ImGuiImplGlfw IM_GLFW = new ImGuiImplGlfw();
    // Renderer backend built on Minecraft 26.2's Blaze3D GPU abstraction (works on OpenGL and Vulkan).
    public static final ImGuiImplBlaze3D IM_BLAZE3D = new ImGuiImplBlaze3D();

    private static final Minecraft client = Minecraft.getInstance();

    public static final Logger LOGGER = LoggerFactory.getLogger("ImGui Integration");

    @Getter
    private static boolean initialized;

    public static void ensureInitialized() {
        if (initialized)
            return;
        RenderSystem.assertOnRenderThread();
        init();
    }

    public static void init() {
        RenderSystem.assertOnRenderThread();
        if (initialized) {
            LOGGER.warn("ImGui has already been initialized!");
            return;
        }

        ImGui.createContext();
        ImPlot.createContext();
        ImGuiEvents.PRE_INIT.invoker().preInit();

        ImGui.getIO().addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard | ImGuiConfigFlags.DockingEnable);
        ImGui.getIO().setConfigMacOSXBehaviors((Util.getPlatform() == Util.OS.OSX));
        if (CraftUI.getConfig().isEnableViewports())
            ImGui.getIO().addConfigFlags(ImGuiConfigFlags.ViewportsEnable);

        ImGui.getIO().setIniFilename(null);

        ImGuiEvents.INIT_IO.invoker().initIO(ImGui.getIO());
        
        IM_GLFW.init(client.getWindow().handle(), true);
        IM_BLAZE3D.init();
        IM_BLAZE3D.newFrame(); // force new frame to init resources before font loading
        ImGuiEvents.POST_INIT.invoker().postInit();
        initialized = true;
    }

}
