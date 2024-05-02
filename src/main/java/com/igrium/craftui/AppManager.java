package com.igrium.craftui;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.craftui.CraftApp.ViewportBounds;
import com.mojang.blaze3d.systems.RenderSystem;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import net.minecraft.client.gl.Framebuffer;

public final class AppManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppManager.class);

    private static final Set<CraftApp> apps = new HashSet<>();

    // Removal queue ensures deals with situations where apps try to remove themselves during the render function.
    private static Set<CraftApp> removalQueue = new HashSet<>();

    /**
     * Get a list of all the apps that are open.
     * @return An unmodifiable view of all open apps.
     */
    public static final Collection<CraftApp> getApps() {
        return Collections.unmodifiableSet(apps);
    }
    
    public static void openApp(CraftApp app) {
        if (app == null) {
            throw new NullPointerException("app may not be null");
        }
        if (apps.contains(app)) {
            LOGGER.warn("CraftApp ({}) is already open!", app);
            return;
        }
        apps.add(app);
        app.onOpen();
    }

    private static boolean rendering;

    public static void preRender(Framebuffer framebuffer) {
        RenderSystem.assertOnRenderThread();
        if (!apps.isEmpty()) {
            rendering = true;
            for (CraftApp app : apps) {
                app.preRender(framebuffer);
            }
            rendering = false;
        }
    }

    public static ViewportBounds getCustomViewportBounds() {
        for (CraftApp app : apps) {
            var bounds = app.getCustomViewportBounds();
            if (bounds != null)
                return bounds;
        }
        return null;
    }

    public static void render(Framebuffer framebuffer) {
        RenderSystem.assertOnRenderThread();

        if (!apps.isEmpty()) {
            if (!ImGuiUtil.isInitialized()) {
                ImGuiUtil.init();
            }

            rendering = true;

            ImGuiUtil.IM_GLFW.newFrame();
            ImGui.newFrame();
        
            for (CraftApp app : apps) {
                app.render(framebuffer);
            }
            
            ImGui.render();
            ImGuiUtil.IM_GL3.renderDrawData(ImGui.getDrawData());

            if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
                long backupWindowPtr = glfwGetCurrentContext();
                ImGui.updatePlatformWindows();
                ImGui.renderPlatformWindowsDefault();
                glfwMakeContextCurrent(backupWindowPtr);
            }

            rendering = false;
        }


        if (!removalQueue.isEmpty()) {
            Iterator<CraftApp> iter = removalQueue.iterator();
            while (iter.hasNext()) {
                CraftApp app = iter.next();
                app.onClose();
                iter.remove();
            }
        }
    }

    public static void closeApp(CraftApp app) {
        if (app == null) {
            throw new NullPointerException("app may not be null");
        }
        if (!apps.contains(app) || removalQueue.contains(app)) {
            LOGGER.warn("CraftApp ({}) is not open!", app);
            return;
        }

        if (rendering) {
            removalQueue.add(app);
        } else {
            app.onClose();
            apps.remove(app);
        }
    }
}
