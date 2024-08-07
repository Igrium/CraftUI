package com.igrium.craftui.font;

import imgui.ImFont;
import net.minecraft.util.Identifier;

/**
 * A collection of "standard" fonts to use with CraftUI
 */
public final class Fonts {
    private Fonts() {}

    public static final Identifier INTER = new Identifier("craftui:inter");
    public static ImFont inter() {
        return ImFontManager.getFont(INTER);
    }

    public static final Identifier INTER_MEDIUM = new Identifier("craftui:inter-medium");
    public static ImFont interMedium() {
        return ImFontManager.getFont(INTER_MEDIUM);
    }

}
