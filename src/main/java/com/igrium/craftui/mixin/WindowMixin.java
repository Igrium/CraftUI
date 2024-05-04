package com.igrium.craftui.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.igrium.craftui.AppManager;
import com.igrium.craftui.CraftApp.ViewportBounds;

import net.minecraft.client.util.Window;

@Mixin(Window.class)
public class WindowMixin {

    @Shadow
    int framebufferWidth;

    @Shadow
    int framebufferHeight;

    @Inject(method = "onFramebufferSizeChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;getFramebufferWidth()I", ordinal = 1))
    void craftui$onFramebufferSizeChanged(long window, int width, int height, CallbackInfo ci) {
        ViewportBounds viewportBounds = AppManager.getCustomViewportBounds();
        if (viewportBounds != null) {
            framebufferWidth = viewportBounds.width();
            framebufferHeight = viewportBounds.height();
        }
    }
}
