package com.igrium.craftui.impl.mixin;

import com.igrium.craftui.impl.render.GameRendererExt;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameRenderer.class)
public class MixinGameRenderer implements GameRendererExt {

    @Shadow
    @Mutable
    private RenderTarget mainRenderTarget;

    @Override
    public void craftui$setMainRenderTarget(RenderTarget target) {
        mainRenderTarget = target;
    }
}
