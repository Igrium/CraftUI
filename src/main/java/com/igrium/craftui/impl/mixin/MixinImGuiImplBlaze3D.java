package com.igrium.craftui.impl.mixin;

import cn.enaium.fabric.imgui.blaze3d.ImGuiImplBlaze3D;
import com.mojang.renderpearl.api.commands.RenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ImGuiImplBlaze3D.class)
public abstract class MixinImGuiImplBlaze3D {

    /**
     * Clamp the scissor rect to the framebuffer so Blaze3D doesn't throw
     * Patch until ImGui Fabric updates; should not conflict if an update is pushed.
     */
    @Redirect(method = "renderDrawData", at= @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/RenderPass;enableScissor(IIII)V"))
    private void craftui$clampScissor(RenderPass renderPass, int x, int y, int width, int height) {
        // If other mods have their own implementation
        if (!(renderPass instanceof AccessorFrontendRenderPass accessor)) return;

        RenderPass.RenderArea area = accessor.getRenderArea();
        if (area == null) {
            renderPass.enableScissor(x, y, width, height);
            return;
        }

        int minX = area.x();
        int minY = area.y();
        int maxX = area.x() + area.width();
        int maxY = area.height();

        int clampedX = Math.max(x, minX);
        int clampedY = Math.max(y, minY);
        int clampedWidth = Math.min(x + width, maxX) - clampedX;
        int clampedHeight = Math.min(y + height, maxY) - clampedY;

        if (clampedWidth <= 0 || clampedHeight <= 0) {

            clampedX = Math.clamp(x, minX, maxX - 1);
            clampedY = Math.clamp(y, minY, maxY - 1);
            clampedWidth = 1;
            clampedHeight = 1;
        }

        renderPass.enableScissor(clampedX, clampedY, clampedWidth, clampedHeight);
    }
}