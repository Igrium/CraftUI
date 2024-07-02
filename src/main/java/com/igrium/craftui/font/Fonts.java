package com.igrium.craftui.font;

import imgui.ImFont;
import net.minecraft.util.Identifier;

/**
 * A collection of "standard" fonts to use with CraftUI
 */
public final class Fonts {
    private Fonts() {}

    private static ImFont inter;
    public static ImFont inter() {
        return inter;
    }

    private static ImFont interMedium;
    public static ImFont interMedium() {
        return interMedium;
    }
    
    public static void reload(ImFontManager manager) {
        inter = manager.get(new Identifier("craftui:inter"));
        interMedium = manager.get(new Identifier("craftui:inter-medium"));
    }
}
