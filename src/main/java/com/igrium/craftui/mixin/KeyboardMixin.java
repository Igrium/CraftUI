package com.igrium.craftui.mixin;

import com.igrium.craftui.app.AppManager;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    void craftui$onKey(long window, int keycode, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (AppManager.wantCaptureKeyboard())
            ci.cancel();
    }

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    void craftui$onChar(long window, int codePoint, int modifiers, CallbackInfo ci) {
        if (AppManager.wantCaptureKeyboard())
            ci.cancel();
    }
}
