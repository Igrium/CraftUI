package com.igrium.craftui.impl.mixin_helper;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;

public interface ExtGameRenderer {
    void craftui$setMainRenderTarget(RenderTarget target);

    static void setMainRenderTarget(GameRenderer renderer, RenderTarget target) {
        ((ExtGameRenderer) renderer).craftui$setMainRenderTarget(target);
    }


}
