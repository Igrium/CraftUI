package com.igrium.craftui.event;

import com.igrium.craftui.font.ImFontManager;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Called when fonts are re-rendered, either due to a resoucepack reload or a UI
 * scale change. If you've cached ImFont objects somewhere, they must be
 * replaced when this is called.
 */
public interface FontReloadCallback {
    public void onFontsReloaded(ImFontManager fontManager);

    /**
     * Called when fonts are re-rendered, either due to a resoucepack reload or a UI
     * scale change. If you've cached ImFont objects somewhere, they must be
     * replaced when this is called.
     */
    public static final Event<FontReloadCallback> EVENT = EventFactory.createArrayBacked(FontReloadCallback.class,
            listeners -> fontManager -> {
                for (var l : listeners) {
                    l.onFontsReloaded(fontManager);
                }
            });
}
