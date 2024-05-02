package com.igrium.craftui;

import net.minecraft.client.gl.Framebuffer;

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
     * @param framebuffer The framebuffer being rendered to.
     */
    protected abstract void render(Framebuffer framebuffer);
    
    /**
     * Called before the game begins to render a frame if this app is active.
     * Updates to framebuffer size should be applied here.
     * 
     * @param framebuffer The framebuffer being rendered to.
     */
    protected void preRender(Framebuffer framebuffer) {

    }

    /**
     * Get the bounds for the main game's viewport.
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
