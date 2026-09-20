package com.igrium.craftui.impl.mixin;

import cn.enaium.fabric.imgui.blaze3d.ImGuiImplBlaze3D;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.commands.RenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ImGuiImplBlaze3D.class)
public abstract class MixinImGuiImplBlaze3D {

    /**
     * Clamp the scissor rect to the framebuffer so Blaze3D doesn't throw
     * Patch until ImGui Fabric updates; should not conflict if an update is pushed.
     */
    @WrapOperation(method = "renderDrawData", at= @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/RenderPass;enableScissor(IIII)V"))
    private void craftui$clampScissor(RenderPass renderPass, int x, int y, int width, int height, Operation<Void> original) {
        // If other mods have their own implementation
        if (!(renderPass instanceof AccessorFrontendRenderPass accessor)) {
            original.call(renderPass, x, y, width, height);
            return;
        }

        RenderPass.RenderArea area = accessor.getRenderArea();
        if (area == null) {
            original.call(renderPass, x, y, width, height);
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

        original.call(renderPass, clampedX, clampedY, clampedWidth, clampedHeight);
    }
}