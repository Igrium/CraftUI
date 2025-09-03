package com.igrium.craftui.impl.mixin;

import com.igrium.craftui.impl.config.CraftUIConfigApp;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class MixinOptionsScreen extends Screen {
    @Shadow public abstract void close();

    private MixinOptionsScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/ThreePartsLayoutWidget;addBody(Lnet/minecraft/client/gui/widget/Widget;)Lnet/minecraft/client/gui/widget/Widget;"))
    void onInit(CallbackInfo ci, @Local GridWidget.Adder adder) {
        adder.add(ButtonWidget.builder(Text.translatable("options.craftui"), b -> {
            var screen = CraftUIConfigApp.createScreen();
            screen.setParent(this);
            if (this.client != null) {
                this.client.setScreen(screen);
            }
        }).build());
    }


}
