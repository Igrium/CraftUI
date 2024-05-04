package com.igrium.craftui;

import net.minecraft.client.MinecraftClient;

/**
 * The base class for every GUI application.
 */
public abstract class CraftApp {

    public static record ViewportBounds(int x, int y, int width, int height) {
        
    }

    protected void onOpen() {

    }

    /**
     * Called after the main Minecraft frame has blit to the primary frame buffer.
     * Primary ImGui calls should be implemented here.
     * 
     * @param client The client
     */
    protected abstract void render(MinecraftClient client);
    
    /**
     * Called before the game begins to render a frame if this app is active.
     * Updates to framebuffer size should be applied here.
     * 
     * @param client The client
     */
    protected void preRender(MinecraftClient client) {

    }

    /**
     * Called directly before the game renders in place of <code>glViewport</code>,
     * allowing the application to override the region of the screen to which the
     * base game draws.
     * 
     * @return Viewport bounds. <code>null</code> to use the default bounds, or
     *         those specified by a lower-priority application.
     */
    protected ViewportBounds getCustomViewportBounds() {
        return null;
    }

    protected void onClose() {

    }
}
