package com.igrium.craftui.impl;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import com.igrium.craftui.api.app.CraftApp;
import com.igrium.craftui.impl.input.CursorLockManager;
import com.igrium.craftui.impl.mixin_helper.ExtWindow;
import com.igrium.craftui.impl.render.ViewportComposite;
import com.igrium.craftui.impl.style.LayoutManager;
import com.igrium.craftui.impl.style.StyleManager;
import com.igrium.craftui.api.style.CraftUILayouts;
import com.igrium.craftui.api.style.CraftUIStyle;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import imgui.ImFont;
import imgui.ImGuiIO;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.craftui.api.app.CraftApp.ViewportBounds;
import com.igrium.craftui.api.style.CraftUIFonts;
import com.mojang.blaze3d.platform.TextInputManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;

import cn.enaium.fabric.imgui.FabricImGui;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;

/**
 * Manages global app state, keeping track of active apps, and rendering.
 * Also houses various global functions such as mouse lock overriding.
 */
@ApiStatus.Internal
public final class AppManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CraftUI/AppManager");

    private static final Set<CraftApp> apps = new HashSet<>();
    private static final Set<CraftApp> appsUnmod = Collections.unmodifiableSet(apps);

    // Queues deal with situations where apps try to add or remove themselves during the render function.
    private static final Queue<CraftApp> addQueue = new ArrayDeque<>();
    private static final Queue<CraftApp> removeQueue = new ArrayDeque<>();


    private static @Nullable ViewportBounds currentViewportBounds;

    /**
     * Owner token for Minecraft's {@link TextInputManager} because SDL3 doesn't send typed chars
     */
    private static final Object TEXT_INPUT_OWNER = new Object();


    /**
     * The full-window composite texture that the final frame should be presented from, or
     * {@code null} to present the game's main render target directly. Non-null only while a
     * custom viewport is active. Consumed by the render-frame present-blit redirect.
     */
    public static @Nullable GpuTextureView getCompositeTextureView() {
        return viewportComposite.getTextureView();
    }

    /**
     * The draw function for a popup that renders over everything else
     */
    @Getter @Setter
    private static Runnable globalPopup;
    private static boolean drawnGlobalPopup;

    /**
     * Disable ImGui from rendering if one of the apps crashed so that Minecraft can write the crash report cleanly
     */
    private static boolean crashed;

    /**
     * Get a list of all the apps that are open.
     * @return An unmodifiable view of all open apps.
     */
    public static Collection<CraftApp> getApps() {
        return appsUnmod;
    }

    private static final ViewportComposite viewportComposite = new ViewportComposite();


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
        if (apps.contains(app) || addQueue.contains(app)) {
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
        if (!apps.contains(app) || removeQueue.contains(app)) {
            LOGGER.warn("CraftApp ({}) is not open!", app);
            return;
        }
        removeQueue.add(app);
    }

    public static boolean isOpen(CraftApp app, boolean includeQueued) {
        if (includeQueued) {
            return (apps.contains(app) || addQueue.contains(app)) && !removeQueue.contains(app);
        } else {
            return apps.contains(app);
        }
    }

    public static boolean isOpen(CraftApp app) {
        return isOpen(app, true);
    }

    @ApiStatus.Internal
    public static void preRender(Minecraft client) {
        RenderSystem.assertOnRenderThread();

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

        CursorLockManager.setForceUnlock(forceMouseUnlock);
        CursorLockManager.onBeginFrame();

        viewportComposite.restoreWorldTarget(client);

        updateViewportBounds(client);

        if (apps.isEmpty())
            return;

        for (CraftApp app : apps) {
            app.preRender(client);
        }

    }

    private static void updateViewportBounds(Minecraft client) {
        currentViewportBounds = null;
        for (CraftApp app : apps) {
            ViewportBounds customBounds = app.getCustomViewportBounds();
            if (customBounds != null) {
                currentViewportBounds = customBounds;
            }
        }

        Window window = client.getWindow();
        var scaled = currentViewportBounds != null ? currentViewportBounds.scaled() : null;
        ExtWindow.setViewportBounds(window, scaled, true);
    }

    public static @Nullable ViewportBounds getCustomViewportBounds() {
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

    /**
     * App-less frames still owed to ImGui after the last app closes. Two are needed because ImGui
     * only retires the active widget the frame <em>after</em> it stops being submitted.
     */
    private static int cleanupFramesRemaining;

    private static final int CLEANUP_FRAMES = 2;

    /**
     * <p>ImGui has a limitation where, if there's a modal popup open, any additional popups will cause it to
     * close unless they're called from within the modal. CraftUIEntrypoint offers a way for mod systems to "inject" popups into
     * other UI paths using the "global popup".</p>
     *
     * <p>If you're rendering a modal and believe an external popup might need to be displayed (file dialog, etc,)
     * call <code>drawGlobalPopup</code> during the modal's render block.</p>
     */
    public static void drawGlobalPopup() {
        if (drawnGlobalPopup) return;
        drawnGlobalPopup = true;

        if (globalPopup != null) globalPopup.run();
    }

    /**
     * Draw all open apps to the screen.
     * @param client Minecraft client instance.
     */
    @ApiStatus.Internal
    public static void render(Minecraft client) {
        RenderSystem.assertOnRenderThread();
        if (crashed)
            return;

        // Deal with Minecraft's bootstrap frame before ImGui is created
        if (!FabricImGui.IMGUI.isCreated())
            return;

        drawnGlobalPopup = false;
        boolean isCleanupFrame = apps.isEmpty();

        if (client.mouseHandler.isMouseGrabbed()) {
            ImGui.getIO().addConfigFlags(ImGuiConfigFlags.NoMouse);
        } else {
            ImGui.getIO().removeConfigFlags(ImGuiConfigFlags.NoMouse);
        }

        forwardInputNextFrame = false;
        forwardMouseInputNextFrame = false;
        forceMouseUnlock = false;

        if (isCleanupFrame && cleanupFramesRemaining <= 0) {
            // Drain events queued by GLFW callbacks while idle so they don't replay once we resume.
            ImGuiIO io = ImGui.getIO();
            io.clearEventsQueue();
            io.clearInputKeys();
            io.clearInputMouse();
            client.textInputManager().stopTextInput(TEXT_INPUT_OWNER);
            return;
        }

        // Styles must be loaded before newFrame (called by FabricImGui)
        StyleManager styleManager = StyleManager.getInstance();
        if (styleManager.isWantStyleUpdate()) {
            CraftUIStyle activeStyle = styleManager.getActiveStyleData();
            activeStyle.buildStyle(ImGui.getStyle());

            Identifier font = activeStyle.getDefaultFont();
            if (font != null) {
                ImFont imFont = CraftUIFonts.getFont(font);
                ImGui.getIO().setFontDefault(imFont);
            }

            styleManager.setWantStyleUpdate(false);
        }

        Identifier desiredLayout = null;
        for (CraftApp app : apps) {
            Identifier l = app.getLayoutPreset();
            if (l != null) {
                desiredLayout = l;
            }
        }
        if (desiredLayout != null) {
            CraftUILayouts.setActiveLayout(desiredLayout);
        }

        LayoutManager layoutManager = LayoutManager.getInstance();
        if (layoutManager.isLayoutUpdate()) {
            ImGui.loadIniSettingsFromMemory(layoutManager.getActiveLayoutData());
            layoutManager.setLayoutUpdate(false);
        }

        if (currentViewportBounds != null) {
            viewportComposite.composite(client, currentViewportBounds.scaled());
        }

        // FabricImGui handles newFrame, etc.
        FabricImGui.IMGUI.draw(_ -> {
            // PRIMARY RENDER
            for (CraftApp app : apps) {
                ImGui.pushID(app.getClass().getCanonicalName().hashCode());
                try {
                    // Re-call getInstance to avoid reallocating lambda due to captured variable
                    app.render(Minecraft.getInstance());
                } catch (Exception e) {
                    crashed = true;
                    CrashReport crashReport = new CrashReport("Error rendering CraftUIEntrypoint app " + app.getClass().getSimpleName(), e);
                    throw new ReportedException(crashReport);
                }
                ImGui.popID();
            }

            try {
                drawGlobalPopup();
            } catch (Exception e) {
                crashed = true;
                CrashReport crashReport = new CrashReport("Error rendering the CraftUIEntrypoint global popup", e);
                throw new ReportedException(crashReport);
            }

            if (isCleanupFrame) {
                ImGui.setWindowFocus(null);
                ImGui.getIO().setWantCaptureKeyboard(false);
                ImGui.getIO().setWantCaptureMouse(false);
            }
        });

        updateTextInput(client);

        if (ImGui.getIO().getWantSaveIniSettings() && CraftUIEntrypoint.getConfig().isLayoutPersistent()) {
            LayoutManager.getInstance().saveUserLayoutData(ImGui.saveIniSettingsToMemory());

            ImGui.getIO().setWantSaveIniSettings(false);
        }

        if (isCleanupFrame) {
            cleanupFramesRemaining--;
        } else {
            cleanupFramesRemaining = CLEANUP_FRAMES;
        }
    }

    /**
     * SDL only emits text input events while text input is started.
     */
    private static void updateTextInput(Minecraft client) {
        TextInputManager textInput = client.textInputManager();
        if (ImGui.getIO().getWantTextInput()) {
            Screen screen = client.gui.screen();
            if (screen != null) {
                screen.clearFocus();
            }
            textInput.startTextInput(TEXT_INPUT_OWNER);
        } else {
            // Only stops if we're the current owner
            textInput.stopTextInput(TEXT_INPUT_OWNER);
        }
    }

    /**
     * If set, mouse inputs will be consumed by the application GUI and should not be processed by Minecraft.
     * @see ImGuiIO#getWantCaptureMouse()
     */
    public static boolean wantCaptureMouse() {
        return !forwardMouseInputNextFrame && ImGui.getIO().getWantCaptureMouse();
    }

    /**
     * If set, keyboard inputs will be consumed by the application GUI and should not be processed by Minecraft.
     * @see ImGuiIO#getWantCaptureKeyboard()
     */
    public static boolean wantCaptureKeyboard() {
        return !forwardInputNextFrame && ImGui.getIO().getWantCaptureKeyboard();
    }
}
