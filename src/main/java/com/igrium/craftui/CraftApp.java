package com.igrium.craftui;

/**
 * The base class for every GUI application.
 */
public abstract class CraftApp {

    protected void onOpen() {

    }

    /**
     * Called after the main Minecraft frame has blit to the primary frame buffer.
     * Primary ImGui calls should be implemented here.
     */
    protected abstract void render();

    protected void onClose() {

    }
}
