package com.igrium.craftui.app;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import com.igrium.craftui.CraftUI;
import com.igrium.craftui.impl.input.CursorLockManager;
import com.igrium.craftui.impl.input.MouseUtils;
import com.igrium.craftui.impl.render.GameRendererExt;
import com.igrium.craftui.impl.render.ViewportBlitter;
import com.igrium.craftui.impl.style.LayoutManager;
import com.igrium.craftui.impl.style.StyleManager;
import com.igrium.craftui.style.CraftUILayouts;
import com.igrium.craftui.style.CraftUIStyle;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import imgui.ImFont;
import imgui.ImGuiIO;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.craftui.app.CraftApp.ViewportBounds;
import com.igrium.craftui.CraftUIFonts;
import com.igrium.craftui.impl.render.ViewportCompositor;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;

import cn.enaium.fabric.imgui.FabricImGui;
import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;

/**
 * Manages global app state, keeping track of active apps, and rendering.
 * Also houses various global functions such as mouse lock overriding.
 */
public final class AppManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("CraftUI/AppManager");

    private static final Set<CraftApp> apps = new HashSet<>();

    // Queues deal with situations where apps try to add or remove themselves during the render function.
    private static final Queue<CraftApp> addQueue = new ArrayDeque<>();
    private static final Queue<CraftApp> removeQueue = new ArrayDeque<>();


    private static @Nullable ViewportBounds currentViewportBounds;


    /**
     * The full-window composite texture that the final frame should be presented from, or
     * {@code null} to present the game's main render target directly. Non-null only while a
     * custom viewport is active. Consumed by the render-frame present-blit redirect.
     */
    public static @Nullable GpuTextureView getCompositeTextureView() {
        return usingComposite ? viewportCompositor.getColorTextureView() : null;
    }

    /**
     * The draw function for a popup that renders over everything else
     */
    @Getter @Setter
    private static @Nullable Runnable globalPopup;
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
        return Collections.unmodifiableSet(apps);
    }

    private static @Nullable RenderTarget worldRenderTarget;
    private static final ViewportCompositor viewportCompositor = new ViewportCompositor();
    private static final ViewportBlitter viewportBlitter = new ViewportBlitter();
    private static boolean usingComposite;

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


        updateViewportBounds(client);

        if (apps.isEmpty())
            return;

        for (CraftApp app : apps) {
            app.preRender(client);
        }

    }

    // TODO: should we only init this system when the first app tries to use it?
    private static void updateViewportBounds(Minecraft client) {
        Window window = client.getWindow();
        int[] realWidth = new int[1];
        int[] realHeight = new int[1];
        GLFW.glfwGetFramebufferSize(window.handle(), realWidth, realHeight);

        if (worldRenderTarget == null) {
            // Take over mainRenderTarget once. From here on, this object is what we'll render into
            RenderTarget original = client.gameRenderer.mainRenderTarget();
            original.destroyBuffers();

            LOGGER.info("Injecting custom window render target");

            worldRenderTarget = new MainTarget(realWidth[0], realHeight[0]);
            GameRendererExt.setMainRenderTarget(client.gameRenderer, worldRenderTarget);
        } else if (usingComposite) {
            GameRendererExt.setMainRenderTarget(client.gameRenderer, worldRenderTarget);
            usingComposite = false;
        }

        ViewportBounds prevViewportBounds = currentViewportBounds;
        currentViewportBounds = null;

        for (CraftApp app : apps) {
            ViewportBounds customBounds = app.getCustomViewportBounds();
            if (customBounds != null) {
                currentViewportBounds = customBounds;
            }
        }

        // GameRenderer derives both the camera's aspect ratio and mainRenderTarget's auto-resize
        // guard from Window.getWidth/getHeight (via windowRenderState), not from the render
        // target's actual texture size. Overriding it here is what actually confines the world
        // render (and its aspect ratio) to the panel. This is safe for ImGui: its display size
        // comes straight from GLFW via ImGuiImplGlfw, not from these cached Window fields.
        if (currentViewportBounds != null) {
            ViewportBounds scaled = currentViewportBounds.scaled();
            window.setWidth(scaled.width());
            window.setHeight(scaled.height());
        } else {
            window.setWidth(realWidth[0]);
            window.setHeight(realHeight[0]);
        }

        if (!Objects.equals(prevViewportBounds, currentViewportBounds)) {
            client.resizeGui();
            client.mouseHandler.setIgnoreFirstMove();
        }

    }

    public static @Nullable ViewportBounds getCustomViewportBounds() {
        return currentViewportBounds;
    }

    /**
     * Return the location on the Minecraft viewport that a given 2D mouse position resides,
     * accounting for custom viewport bounds.
     *
     * @param globalX Mouse X position relative to the main window.
     * @param globalY Mouse Y position relative to the main window.
     * @return The equivalent position relative to the Minecraft viewport (appropriate to sending to ingame UI)
     */
    public static Vector2d getViewportMousePos(double globalX, double globalY) {
        ViewportBounds viewportBounds = getCustomViewportBounds();
        if (viewportBounds == null) {
            return new Vector2d(globalX, globalY);
        }

        return MouseUtils.calculateViewportMouse(Minecraft.getInstance().getWindow(), viewportBounds, globalX, globalY);
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
     * <p>ImGui has a limitation where, if there's a modal popup open, any additional popups will cause it to
     * close unless they're called from within the modal. CraftUI offers a way for mod systems to "inject" popups into
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
    public static void render(Minecraft client) {
        RenderSystem.assertOnRenderThread();
        if (crashed)
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

        if (isCleanupFrame && !needsCleanupFrame) {
            // Drain events queued by GLFW callbacks while idle so they don't replay once we resume.
            ImGui.getIO().clearEventsQueue();
            return;
        }

        // STYLE and LAYOUT must be applied *before* draw(), because the library's draw() calls
        // ImGui.newFrame() before invoking this callback. newFrame() reads io.FontDefault to fix the
        // frame's font size and expects ini settings to be loaded between frames, so setting them
        // inside the callback would apply the default font a frame late and load ini mid-frame.
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

        // The library's draw() runs ImGui.newFrame() before and ImGui.render() + backend draw after
        // this callback, so all widget-emitting work happens inside it. Multi-viewport platform
        // windows are handled by the library too.

        if (currentViewportBounds != null) {
            compositeViewportTarget(client);
        }

        FabricImGui.IMGUI.draw(io -> {
            // PRIMARY RENDER
            for (CraftApp app : apps) {
                ImGui.pushID(app.getClass().getCanonicalName().hashCode());
                try {
                    app.render(client);
                } catch (Exception e) {
                    crashed = true;
                    CrashReport crashReport = new CrashReport("Error rendering CraftUI app " + app.getClass().getSimpleName(), e);
                    throw new ReportedException(crashReport);
                }
                ImGui.popID();
            }

            try {
                drawGlobalPopup();
            } catch (Exception e) {
                crashed = true;
                CrashReport crashReport = new CrashReport("Error rendering the CraftUI global popup", e);
                throw new ReportedException(crashReport);
            }

            if (isCleanupFrame) {
                ImGui.setWindowFocus(null);
                ImGui.getIO().setWantCaptureKeyboard(false);
                ImGui.getIO().setWantCaptureMouse(false);
            }
        });

        if (ImGui.getIO().getWantSaveIniSettings() && CraftUI.getConfig().isLayoutPersistent()) {
            LayoutManager.getInstance().saveUserLayoutData(ImGui.saveIniSettingsToMemory());

            ImGui.getIO().setWantSaveIniSettings(false);
        }

        needsCleanupFrame = !isCleanupFrame;
    }

    private static void compositeViewportTarget(Minecraft client) {
        if (currentViewportBounds == null || worldRenderTarget == null) {
            return; // shouldn't happen
        }

        Window window = client.getWindow();

        int[] realWidth = new int[1];
        int[] realHeight = new int[1];
        GLFW.glfwGetFramebufferSize(window.handle(), realWidth, realHeight);

        ViewportBounds scaled = currentViewportBounds.scaled();

        viewportCompositor.ensureSize(realWidth[0], realHeight[0]);

        // ViewportBounds.y is bottom-left origin (see DockSpaceApp.beginViewport), but the blit
        // target's destY is top-left-origin screen pixels, so convert here.
        int destY = realHeight[0] - scaled.y() - scaled.height();

        // Draw the world render as a textured quad rather than a raw same-size pixel copy: a quad
        // always fills the destination rect exactly (resampling to fit), so if worldRenderTarget's
        // actual size ever drifts a frame behind the panel's current bounds, it self-corrects instead
        // of leaving a visible gap or sampling the wrong region.
        viewportBlitter.blit(viewportCompositor.getColorTextureView(), realWidth[0], realHeight[0],
                worldRenderTarget.getColorTextureView(), scaled.x(), destY, scaled.width(), scaled.height());

        GameRendererExt.setMainRenderTarget(client.gameRenderer, viewportCompositor);
        usingComposite = true;
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
