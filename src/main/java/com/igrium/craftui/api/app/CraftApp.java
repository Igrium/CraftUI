package com.igrium.craftui.api.app;

import com.igrium.craftui.api.event.UIEvent;

import com.igrium.craftui.api.CraftUI;
import imgui.ImGui;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The base class for every GUI application.
 */
public abstract class CraftApp {

    public record ViewportBounds(int x, int y, int width, int height) {
        public ViewportBounds scaled(float scaleX, float scaleY) {
            return new ViewportBounds((int) (x * scaleX), (int) (y * scaleY), (int) (width * scaleX), (int) (height * scaleY));
        }

        /**
         * Scale the viewport bounds to real pixels from logical units to deal with high-dpi displays
         */
        public ViewportBounds scaled() {
            return scaled(ImGui.getIO().getDisplayFramebufferScaleX(), ImGui.getIO().getDisplayFramebufferScaleY());
        }
    }

    @Getter
    @Accessors(fluent = true)
    private final UIEvent<Runnable> openEvent = UIEvent.ofRunnable();

    @Getter
    @Accessors(fluent = true)
    private final UIEvent<Runnable> closeEvent = UIEvent.ofRunnable();

    @ApiStatus.Internal
    public final void onOpen() {
        openEvent.invoker().run();
    }

    /**
     * Called after the main Minecraft frame has blit to the primary frame buffer.
     * Primary ImGui calls should be implemented here.
     * 
     * @param client The client
     */
    public abstract void render(Minecraft client);
    
    /**
     * Called before the game begins to render a frame if this app is active.
     * Updates to framebuffer size should be applied here.
     * 
     * @param client The client
     */
    public void preRender(Minecraft client) {

    }

    /**
     * Called directly before the game renders in place of <code>glViewport</code>,
     * allowing the application to override the region of the screen to which the
     * base game draws.
     *
     * @return Viewport bounds. <code>null</code> to use the default bounds, or
     *         those specified by a lower-priority application.
     */
    public @Nullable ViewportBounds getCustomViewportBounds() {
        return null;
    }

    /**
     * <p>Indicate to the app manager that this app wants to use a specific layout preset.</p>
     * <p>Note that, due to technical constraints, only one layout can be active across all of CraftUI.
     * If multiple apps use this function, only the highest-priority app's layout will be used.</p>
     *
     * @return The desired layout. <code>null</code> to use the default layout or those specified
     *         by a lower-priority application.
     */
    public @Nullable Identifier getLayoutPreset() {
        return null;
    }

    @ApiStatus.Internal
    public final void onClose() {
        closeEvent.invoker().run();
    }

    public final boolean isOpen() {
        return CraftUI.isOpen(this);
    }

    /**
     * Close this app. Shortcut for <code>CraftUI.closeApp(this)</code>
     */
    public final void close() {
        CraftUI.closeApp(this);
    }
}
