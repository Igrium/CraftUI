package com.igrium.craftui.impl.config;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Called when the CraftUI config is (re-)loaded
 */
@Deprecated
public interface CraftUIConfigCallback {
    Event<CraftUIConfigCallback> EVENT = EventFactory.createArrayBacked(CraftUIConfigCallback.class,
            listeners -> config -> {
                for (var l : listeners) {
                    l.onUpdateConfig(config);
                }
            });

    void onUpdateConfig(CraftUIConfig config);
}
