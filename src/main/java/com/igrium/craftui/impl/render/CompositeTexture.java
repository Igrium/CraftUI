package com.igrium.craftui.impl.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.GpuFormat;


public class CompositeTexture extends RenderTarget {
    public CompositeTexture() {
        super("CraftUIEntrypoint Viewport Compositor", GpuFormat.RGBA8_UNORM, null);
    }

    public void ensureSize(int width, int height) {
        if (width != this.width || height != this.height) {
            resize(width, height);
        }
    }
}
