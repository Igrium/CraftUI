package com.igrium.craftui.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.igrium.craftui.app.AppManager;
import com.igrium.craftui.app.CraftApp.ViewportBounds;
import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(Framebuffer.class)
public class FramebufferMixin {

    @Shadow
    protected int colorAttachment;

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    void onDraw(int width, int height, CallbackInfo ci) {
        ViewportBounds customBounds = AppManager.getCustomViewportBounds();
        if (isMainFrameBuffer() && customBounds != null) {
            RenderSystem.assertOnRenderThread();
            GlStateManager._colorMask(true, true, true, false);
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask(false);
            GlStateManager._viewport(customBounds.x(), customBounds.y(), customBounds.width(), customBounds.height());
            ShaderProgram shaderProgram = Objects.requireNonNull(RenderSystem.setShader(ShaderProgramKeys.BLIT_SCREEN), "Blit shader not loaded");
            shaderProgram.addSamplerTexture("InSampler", this.colorAttachment);
            BufferBuilder bufferBuilder = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.BLIT_SCREEN);
            bufferBuilder.vertex(0.0F, 0.0F, 0.0F);
            bufferBuilder.vertex(1.0F, 0.0F, 0.0F);
            bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
            bufferBuilder.vertex(0.0F, 1.0F, 0.0F);
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            GlStateManager._depthMask(true);
            GlStateManager._colorMask(true, true, true, true);
            ci.cancel();
        }
    }

    @Unique
    private boolean isMainFrameBuffer() {
        return (Object)this == MinecraftClient.getInstance().getFramebuffer();
    }
}
