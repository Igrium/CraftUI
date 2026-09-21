package com.igrium.craftui.impl.mixin;

import com.mojang.renderpearl.backend.opengl.DirectStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlCommandEncoder")
public class MixinGlCommandEncoder {
    // Mojang fucked up and forgot to add destX and destY to the blit end point
    @Redirect(method = "copyTextureToTexture", at = @At(value = "INVOKE",
            target = "Lcom/mojang/renderpearl/backend/opengl/DirectStateAccess;blitFrameBuffers(IIIIIIIIIIII)V"))
    void fixCopyTextureToTexture(DirectStateAccess instance, int readFbo, int drawFbo,
                                 int sourceX, int sourceY,
                                 int width, int height,
                                 int destX, int destY,
                                 int width2, int height2,
                                 int mask, int filter) {
        instance.blitFrameBuffers(readFbo, drawFbo, sourceX, sourceY, width, height, destX, destY,
                width2 + destX, height2 + destY, mask, filter);
    }
}
