package com.igrium.craftui.app;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import com.igrium.craftui.input.CursorLockManager;
import imgui.ImGuiIO;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.craftui.app.CraftApp.ViewportBounds;
import com.igrium.craftui.font.Fonts;
import com.igrium.craftui.render.ImGuiUtil;
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

    /**
     * Get a list of all the apps that are open.
     * @return An unmodifiable view of all open apps.
     */
    public static Collection<CraftApp> getApps() {
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

        CursorLockManager.onBeginFrame();

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

        ViewportBounds prevViewportBounds = currentViewportBounds;
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

    private static boolean forwardInputNextFrame;

    /**
     * Forward keyboard input to the game next frame, even if we have a widget focused.
     * Useful for viewport controls.
     */
    public static void forwardInputNextFrame() {
        forwardInputNextFrame = true;
    }

    private static boolean forwardMouseInputNextFrame;

    /**
     * Forward mouse input to the game next frame, even if we have a widget focused.
     * Useful for viewport controls.
     */
    public static void forwardMouseInputNextFrame() {
        forwardMouseInputNextFrame = true;
    }

    private static boolean forceMouseUnlock;

    /**
     * Force the mouse to be unlocked this frame, regardless of what Minecraft thinks.
     */
    public static void forceMouseUnlock() {
        forceMouseUnlock = true;
    }

    private static boolean needsCleanupFrame;

    /**
     * Draw all open apps to the screen.
     * @param client Minecraft client instance.
     */
    public static void render(MinecraftClient client) {
        RenderSystem.assertOnRenderThread();
        boolean isCleanupFrame = apps.isEmpty();

        if (client.mouse.isCursorLocked()) {
            ImGui.getIO().addConfigFlags(ImGuiConfigFlags.NoMouse);
        } else {
            ImGui.getIO().removeConfigFlags(ImGuiConfigFlags.NoMouse);
        }

        forwardInputNextFrame = false;
        forwardMouseInputNextFrame = false;
        forceMouseUnlock = false;

        if (isCleanupFrame && !needsCleanupFrame)
            return;


        ImGuiUtil.IM_GLFW.newFrame();
        ImGui.newFrame();
    
        ImGui.pushFont(Fonts.inter());

        for (CraftApp app : apps) {
            ImGui.pushID(app.getClass().getCanonicalName().hashCode());
            try {
                app.render(client);
            } catch (Exception e) {
                CrashReport crashReport = new CrashReport("Error rendering CraftUI app " + app.getClass().getSimpleName(), e);
                throw new CrashException(crashReport);
            }
            ImGui.popID();
        }

        ImGui.popFont();

        if (isCleanupFrame) {
            ImGui.setWindowFocus(null);
            ImGui.getIO().setWantCaptureKeyboard(false);
            ImGui.getIO().setWantCaptureMouse(false);
        }

        ImGui.render();
        ImGuiUtil.IM_GL3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            long backupWindowPtr = glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            glfwMakeContextCurrent(backupWindowPtr);
        }

        needsCleanupFrame = !isCleanupFrame;
        CursorLockManager.setForceUnlock(forceMouseUnlock);
    }

    /**
     * If set, mouse inputs will be consumed by the application GUI and should not be processed by Minecraft.
     * @see ImGuiIO#getWantCaptureMouse()
     */
    public static boolean wantCaptureMouse() {
        return !forwardMouseInputNextFrame && ImGui.getIO().getWantCaptureKeyboard();
    }

    /**
     * If set, keyboard inputs will be consumed by the application GUI and should not be processed by Minecraft.
     * @see ImGuiIO#getWantCaptureKeyboard()
     */
    public static boolean wantCaptureKeyboard() {
        return !forwardInputNextFrame && ImGui.getIO().getWantCaptureKeyboard();
    }
}
