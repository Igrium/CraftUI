package com.igrium.craftui.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.igrium.craftui.AppManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow
    @Final
    Framebuffer framebuffer;


    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;draw(II)V", shift = Shift.AFTER))
    void craftui$afterMainBlit(boolean tick, CallbackInfo ci) {
        AppManager.render(framebuffer);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMeasuringTimeNano()J", shift = Shift.AFTER))
    void craftui$preRender(boolean tick, CallbackInfo ci) {
        AppManager.preRender(framebuffer);
    }
}
