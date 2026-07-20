package com.igrium.craftui.impl.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.igrium.craftui.app.AppManager;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Gui;

/**
 * Intercept mouse re-grab
 */
@Mixin(Gui.class)
public class GuiMixin {

    @Redirect(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;grabMouse()V"))
    void craftui$onSetScreen(MouseHandler instance) {
        if (!AppManager.wantCaptureMouse())
            instance.grabMouse();
    }
}
