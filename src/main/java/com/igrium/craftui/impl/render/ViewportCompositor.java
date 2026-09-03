package com.igrium.craftui.impl.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.jspecify.annotations.Nullable;


public class ViewportCompositor extends RenderTarget {
    public ViewportCompositor() {
        super("CraftUIEntrypoint Viewport Compositor", false, GpuFormat.RGBA8_UNORM);
    }

    public void ensureSize(int width, int height) {
        if (this.width == width && this.height == height && getColorTexture() != null) {
            return;
        }
        resize(width, height);
    }
}
