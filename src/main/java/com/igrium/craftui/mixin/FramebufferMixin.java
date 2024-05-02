package com.igrium.craftui.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.igrium.craftui.AppManager;
import com.igrium.craftui.CraftApp.ViewportBounds;
import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.gl.Framebuffer;

@Mixin(Framebuffer.class)
public class FramebufferMixin {

    @Redirect(method = "drawInternal", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_viewport(IIII)V"))
    void craftui$overrideViewport(int x, int y, int width, int height) {
        ViewportBounds customBounds = AppManager.getCustomViewportBounds();
        if (customBounds != null) {
            GlStateManager._viewport(customBounds.x(), customBounds.y(), customBounds.width(), customBounds.height());
        } else {
            GlStateManager._viewport(x, y, width, height);
        }
    }
}
