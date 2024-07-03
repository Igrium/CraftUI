package com.igrium.craftui;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.craftui.CraftApp.ViewportBounds;
import com.igrium.craftui.font.Fonts;
import com.mojang.blaze3d.systems.RenderSystem;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

public final class AppManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppManager.class);

    private static final Set<CraftApp> apps = new HashSet<>();

    // Queues deal with situations where apps try to add or remove themselves during the render function.
    private static final Queue<CraftApp> addQueue = new ArrayDeque<>();
    private static final Queue<CraftApp> removeQueue = new ArrayDeque<>();

    private static ViewportBounds currentViewportBounds;
    private static ViewportBounds prevViewportBounds;

    /**
     * Get a list of all the apps that are open.
     * @return An unmodifiable view of all open apps.
     */
    public static final Collection<CraftApp> getApps() {
        return Collections.unmodifiableSet(apps);
    }
    
    /**
     * Queue an app for opening. App will be opened at the beginning of the next render cycle.
     * @param app The app to open. May not be <code>null</code>.
     */
    public static void openApp(CraftApp app) {
        RenderSystem.assertOnRenderThread();
        if (app == null) {
            throw new NullPointerException("app may not be null.");
        }
        removeQueue.remove(app);
        if (app.isOpen() || addQueue.contains(app)) {
            LOGGER.warn("CraftApp ({}) is already open!", app);
            return;
        }
        addQueue.add(app);
    }

    /**
     * Queue an app for closing. App will be closed at the beginning of the next render cycle.
     * @param app The app to close. May not be <code>null</code>
     */
    public static void closeApp(CraftApp app) {
        RenderSystem.assertOnRenderThread();
        if (app == null)
            return;
        addQueue.remove(app);
        if (!app.isOpen() || removeQueue.contains(app)) {
            LOGGER.warn("CraftApp ({}) is not open!", app);
            return;
        }
        removeQueue.add(app);
    }

    public static void preRender(MinecraftClient client) {
        RenderSystem.assertOnRenderThread();

        if (!ImGuiUtil.isInitialized()) {
            ImGuiUtil.init();
        }

        while (!removeQueue.isEmpty()) {
            CraftApp app = removeQueue.poll();
            app.onClose();
            apps.remove(app);
        }

        while (!addQueue.isEmpty()) {
            CraftApp app = addQueue.poll();
            apps.add(app);
            app.onOpen();
        }
        
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
    }

    public static void render(MinecraftClient client) {
        RenderSystem.assertOnRenderThread();

        if (apps.isEmpty()) return;

        ImGuiUtil.IM_GLFW.newFrame();
        ImGui.newFrame();
    
        ImGui.pushFont(Fonts.inter());
        for (CraftApp app : apps) {
            app.render(client);
        }
        ImGui.popFont();

        ImGui.render();
        ImGuiUtil.IM_GL3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            long backupWindowPtr = glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            glfwMakeContextCurrent(backupWindowPtr);
        }
    }
}
