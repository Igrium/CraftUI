package com.igrium.craftui;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.craftui.CraftApp.ViewportBounds;
import com.mojang.blaze3d.systems.RenderSystem;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

public final class AppManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppManager.class);

    private static final Set<CraftApp> apps = new HashSet<>();

    // Removal queue ensures deals with situations where apps try to remove themselves during the render function.
    private static Set<CraftApp> removalQueue = new HashSet<>();

    private static ViewportBounds currentViewportBounds;
    private static ViewportBounds prevViewportBounds;

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

    public static void preRender(MinecraftClient client) {
        RenderSystem.assertOnRenderThread();

        
        rendering = true;
        prevViewportBounds = currentViewportBounds;
        currentViewportBounds = null;

        for (CraftApp app : apps) {
            ViewportBounds customBounds = app.getCustomViewportBounds();
            if (customBounds != null) {
                currentViewportBounds = customBounds;
            }
        }

        if (!Objects.equals(prevViewportBounds, currentViewportBounds)) {
            updateViewportBounds(client);
        }

        if (apps.isEmpty())
            return;

        for (CraftApp app : apps) {
            app.preRender(client);
        }

        rendering = false;
    }

    private static void updateViewportBounds(MinecraftClient client) {
        Window window = client.getWindow();
        if (currentViewportBounds != null) {
            window.setFramebufferWidth(currentViewportBounds.width());
            window.setFramebufferHeight(currentViewportBounds.height());
        } else {
            window.setFramebufferWidth(window.getWidth());
            window.setFramebufferHeight(window.getHeight());
        }

        client.onResolutionChanged();
        client.mouse.onResolutionChanged();
    }

    public static ViewportBounds getCustomViewportBounds() {
        return currentViewportBounds;
        // for (CraftApp app : apps) {
        //     var bounds = app.getCustomViewportBounds();
        //     if (bounds != null)
        //         return bounds;
        // }
        // return null;
    }

    public static void render(MinecraftClient client) {
        RenderSystem.assertOnRenderThread();

        if (!apps.isEmpty()) {
            if (!ImGuiUtil.isInitialized()) {
                ImGuiUtil.init();
            }

            rendering = true;

            ImGuiUtil.IM_GLFW.newFrame();
            ImGui.newFrame();
        
            for (CraftApp app : apps) {
                app.render(client);
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
