package com.igrium.craftui.event;

import imgui.ImGuiIO;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public class ImGuiEvents {
    public static interface PreInit {
        public void preInit();
    }

    public static interface PostInit {
        public void postInit();
    }

    public static interface InitIO {
        public void initIO(ImGuiIO io);
    }

    public static final Event<PreInit> PRE_INIT = EventFactory.createArrayBacked(PreInit.class,
            listeners -> () -> {
                for (var l : listeners) {
                    l.preInit();
                }
            });

    public static final Event<PostInit> POST_INIT = EventFactory.createArrayBacked(PostInit.class,
            listeners -> () -> {
                for (var l : listeners) {
                    l.postInit();
                }
            });

    public static final Event<InitIO> INIT_IO = EventFactory.createArrayBacked(InitIO.class,
            listeners -> io -> {
                for (var l : listeners) {
                    l.initIO(io);
                }
            });
}
