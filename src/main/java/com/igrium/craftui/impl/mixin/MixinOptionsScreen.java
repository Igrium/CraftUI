package com.igrium.craftui.impl.mixin;

import com.igrium.craftui.impl.config.CraftUIConfigApp;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class MixinOptionsScreen extends Screen {
    @Shadow public abstract void onClose();

    private MixinOptionsScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToContents(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    void onInit(CallbackInfo ci, @Local GridLayout.RowHelper adder) {
        adder.addChild(Button.builder(Component.translatable("options.craftui"), b -> {
            var screen = CraftUIConfigApp.createScreen();
            screen.setParent(this);
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(screen);
            }
        }).build());
    }


}
