package com.igrium.craftui.impl.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.igrium.craftui.app.AppManager;
import com.igrium.craftui.app.CraftApp.ViewportBounds;
import com.mojang.blaze3d.platform.Window;

@Mixin(Window.class)
public class MixinWindow {

    @Shadow
    private int framebufferWidth;

    @Shadow
    private int framebufferHeight;

    @Inject(method = "onFramebufferResize", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/WindowEventHandler;framebufferSizeChanged()V", shift = At.Shift.BEFORE))
    void craftui$onFramebufferSizeChanged(long handle, int newWidth, int newHeight, CallbackInfo ci) {
        ViewportBounds viewportBounds = AppManager.getCustomViewportBounds();
        if (viewportBounds != null) {
            viewportBounds = viewportBounds.scaled();
            framebufferWidth = viewportBounds.width();
            framebufferHeight = viewportBounds.height();
        }
    }
}
