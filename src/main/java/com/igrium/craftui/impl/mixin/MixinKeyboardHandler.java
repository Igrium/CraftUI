package com.igrium.craftui.impl.mixin;

import com.igrium.craftui.app.AppManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    void craftui$onKey(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (AppManager.wantCaptureKeyboard())
            ci.cancel();
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    void craftui$onChar(long handle, CharacterEvent event, CallbackInfo ci) {
        if (AppManager.wantCaptureKeyboard())
            ci.cancel();
    }
}
