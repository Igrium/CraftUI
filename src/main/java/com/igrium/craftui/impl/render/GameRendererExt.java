package com.igrium.craftui.impl.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;

public interface GameRendererExt {
    void craftui$setMainRenderTarget(RenderTarget target);

    static void setMainRenderTarget(GameRenderer renderer, RenderTarget target) {
        ((GameRendererExt) renderer).craftui$setMainRenderTarget(target);
    }


}
