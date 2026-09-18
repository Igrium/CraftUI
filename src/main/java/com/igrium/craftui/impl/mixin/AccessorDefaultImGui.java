package com.igrium.craftui.impl.mixin;

import cn.enaium.fabric.imgui.DefaultImGui;
import cn.enaium.fabric.imgui.blaze3d.ImGuiImplBlaze3D;
import imgui.gl3.ImGuiImplGl3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DefaultImGui.class)
public interface AccessorDefaultImGui {
    @Accessor("imGuiImplGl3")
    ImGuiImplGl3 getImGuiImplGl3();

    @Accessor("imGuiImplBlaze3D")
    ImGuiImplBlaze3D getImGuiImplBlaze3D();
}
