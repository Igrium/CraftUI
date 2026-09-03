package com.igrium.craftui.impl.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.igrium.craftui.impl.AppManager;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuSurface;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    // Draw the ImGui overlay into the main render target right before it is blitted to the window surface,
    // so it composites on top of the finished game frame (works on both the OpenGL and Vulkan backends).
    @Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/GpuSurface;blitFromTexture(Lcom/mojang/blaze3d/systems/CommandEncoder;Lcom/mojang/blaze3d/textures/GpuTextureView;)V", shift = Shift.BEFORE))
    void craftui$afterMainBlit(boolean advanceGameTime, CallbackInfo ci) {
        AppManager.render((Minecraft) (Object) this);
    }

    // When a custom viewport is active, present the full-window composite texture (game confined to
    // its sub-rectangle, with ImGui drawn over it) instead of the shrunken game render target.
    // Runs after craftui$afterMainBlit, which populates the composite for this frame.
    @Redirect(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/GpuSurface;blitFromTexture(Lcom/mojang/blaze3d/systems/CommandEncoder;Lcom/mojang/blaze3d/textures/GpuTextureView;)V"))
    void craftui$redirectPresentBlit(GpuSurface surface, CommandEncoder commandEncoder, GpuTextureView textureView) {
        GpuTextureView composite = AppManager.getCompositeTextureView();
        surface.blitFromTexture(commandEncoder, composite != null ? composite : textureView);
    }

    // Run app pre-render logic near the start of the frame, right after the render-start timer is sampled.
    @Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getNanos()J", ordinal = 0, shift = Shift.AFTER))
    void craftui$preRender(boolean advanceGameTime, CallbackInfo ci) {
        AppManager.preRender((Minecraft) (Object) this);
    }
}
