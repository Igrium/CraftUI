package com.igrium.craftui.event;

import imgui.ImGuiIO;

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

    public static final Event<PreInit> PRE_INIT = Event.createArrayBacked(
            listeners -> () -> {
                for (var l : listeners) {
                    l.preInit();
                }
            });

    public static final Event<PostInit> POST_INIT = Event.createArrayBacked(
            listeners -> () -> {
                for (var l : listeners) {
                    l.postInit();
                }
            });

    public static final Event<InitIO> INIT_IO = Event.createArrayBacked(
            listeners -> io -> {
                for (var l : listeners) {
                    l.initIO(io);
                }
            });
}
