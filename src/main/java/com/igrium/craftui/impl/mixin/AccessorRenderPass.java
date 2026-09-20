package com.igrium.craftui.impl.mixin;

import com.mojang.blaze3d.systems.RenderPass;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderPass.class)
public interface AccessorRenderPass {
    @Accessor("renderArea")
    RenderPass.@Nullable RenderArea getRenderArea();
}