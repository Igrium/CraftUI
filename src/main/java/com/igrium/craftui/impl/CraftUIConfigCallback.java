package com.igrium.craftui.impl;

import com.igrium.craftui.config.CraftUIConfig;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Called when the CraftUI config is (re-)loaded
 */
public interface CraftUIConfigCallback {
    Event<CraftUIConfigCallback> EVENT = EventFactory.createArrayBacked(CraftUIConfigCallback.class,
            listeners -> config -> {
                for (var l : listeners) {
                    l.onUpdateConfig(config);
                }
            });

    void onUpdateConfig(CraftUIConfig config);
}
