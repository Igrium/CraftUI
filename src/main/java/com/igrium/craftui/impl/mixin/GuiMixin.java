package com.igrium.craftui.impl.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.igrium.craftui.app.AppManager;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Gui;

/**
 * In 26.2 the screen-switching logic (and the mouse re-grab that used to live in Minecraft.setScreen)
 * moved to {@link Gui#setScreen}. This intercepts the re-grab so ImGui can keep input possession.
 */
@Mixin(Gui.class)
public class GuiMixin {

    @Redirect(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;grabMouse()V"))
    void craftui$onSetScreen(MouseHandler instance) {
        // Only grab if imgui doesn't have input possession.
        if (!AppManager.wantCaptureKeyboard())
            instance.grabMouse();
    }
}
