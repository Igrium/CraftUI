package com.igrium.craftui.impl.render;

import com.igrium.craftui.api.app.CraftApp.ViewportBounds;
import com.igrium.craftui.impl.mixin_helper.ExtGameRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * Owns the swap between the game's normal (world-only) render target and a full-window
 * composite target while a custom viewport is active.
 */
public class ViewportComposite {
    private @Nullable RenderTarget worldRenderTarget;
    private final CompositeTexture compositeTexture = new CompositeTexture();
    private boolean active;
    
    public void restoreWorldTarget(Minecraft client) {
        if (active) {
            ExtGameRenderer.setMainRenderTarget(client.gameRenderer, worldRenderTarget);
            active = false;
        }
    }

    /**
     * Copy the (shrunk) world render into the correct sub-rect of the full-window composite
     * target.
     */
    public void composite(Minecraft client, ViewportBounds bounds) {
        RenderTarget world = client.gameRenderer.mainRenderTarget();
        if (worldRenderTarget == null) {
            worldRenderTarget = world;
        }

        Window window = client.getWindow();
        // Real (not overridden) framebuffer size
        Window.FramebufferSize size = window.queryFramebufferSize();
        int realWidth = size.width();
        int realHeight = size.height();

        compositeTexture.ensureSize(realWidth, realHeight);

        RenderSystem.assertOnRenderThread();
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        GpuTexture fromTexture = world.getColorTexture();
        GpuTexture toTexture = compositeTexture.getColorTexture();


        if (fromTexture != null && toTexture != null) {
            // Because apparently it's better to crash than clamp in copyTextureToTexture
            int width = Math.min(bounds.width(), toTexture.getWidth(0) - bounds.x());
            int height = Math.min(bounds.height(), toTexture.getHeight(0) - bounds.y());

            encoder.copyTextureToTexture(fromTexture, toTexture,
                    0, bounds.x(), bounds.y(), 0, 0, width, height);
        }

        ExtGameRenderer.setMainRenderTarget(client.gameRenderer, compositeTexture);
        active = true;
    }
    
    public @Nullable GpuTextureView getTextureView() {
        return active ? compositeTexture.getColorTextureView() : null;
    }
}