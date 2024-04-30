package com.igriu.craftui.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.igriu.craftui.test.ImGuiTestApp;

import net.minecraft.client.MinecraftClient;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;draw(II)V", shift = Shift.AFTER))
    void craftui$afterMainBlit(boolean tick, CallbackInfo ci) {
        if (ImGuiTestApp.isOpen()) {
            ImGuiTestApp.render();
        }
    }
}
