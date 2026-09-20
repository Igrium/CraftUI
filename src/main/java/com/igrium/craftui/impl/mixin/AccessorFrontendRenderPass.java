package com.igrium.craftui.impl.mixin;

import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FrontendRenderPass.class)
public interface AccessorFrontendRenderPass {
    @Accessor("renderArea")
    RenderPass.@Nullable RenderArea getRenderArea();
}
