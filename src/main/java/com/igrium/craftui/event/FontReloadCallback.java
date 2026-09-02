package com.igrium.craftui.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Called when fonts are re-rendered, either due to a resourcepack reload or a UI
 * scale change. If you've cached ImFont objects somewhere, they must be
 * replaced when this is called.
 */
public interface FontReloadCallback {
    void onFontsReloaded();

    /**
     * Called when fonts are re-rendered, either due to a resourcepack reload or a UI
     * scale change. If you've cached ImFont objects somewhere, they must be
     * replaced when this is called.
     */
    Event<FontReloadCallback> EVENT = EventFactory.createArrayBacked(FontReloadCallback.class, listeners -> () -> {
        for (var l : listeners) {
            l.onFontsReloaded();
        }
    });
}
