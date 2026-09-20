package com.igrium.craftui.impl.mixin;

import com.igrium.craftui.impl.AppManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    
    @Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuSurface;blitFromTexture(Lcom/mojang/renderpearl/api/commands/CommandEncoder;Lcom/mojang/renderpearl/api/textures/GpuTextureView;)V", shift = Shift.BEFORE))
    void craftui$afterMainBlit(boolean advanceGameTime, CallbackInfo ci) {
        AppManager.render((Minecraft) (Object) this);
    }

    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE",
            target = "Lcom/mojang/renderpearl/api/device/GpuSurface;blitFromTexture(Lcom/mojang/renderpearl/api/commands/CommandEncoder;Lcom/mojang/renderpearl/api/textures/GpuTextureView;)V"))
    void craftui$presentBlit(GpuSurface instance, CommandEncoder encoder, GpuTextureView texView, Operation<Void> original) {
        GpuTextureView composite = AppManager.getCompositeTextureView();
        original.call(instance, encoder, composite != null ? composite : texView);
    }

    // Run app pre-render logic near the start of the frame, right after the render-start timer is sampled.
    @Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getNanos()J", ordinal = 0, shift = Shift.AFTER))
    void craftui$preRender(boolean advanceGameTime, CallbackInfo ci) {
        AppManager.preRender((Minecraft) (Object) this);
    }
}