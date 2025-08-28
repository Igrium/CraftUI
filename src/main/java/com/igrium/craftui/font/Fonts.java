package com.igrium.craftui.font;

import com.igrium.craftui.impl.font.ImFontManager;
import imgui.ImFont;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * Interface to access standard CraftUI fonts.
 */
public final class Fonts {
    private Fonts() {}

    public static final Identifier INTER = Identifier.of("craftui:inter");
    public static ImFont inter() {
        return ImFontManager.getInstance().get(INTER);
    }

    public static final Identifier INTER_MEDIUM = Identifier.of("craftui:inter-medium");
    public static ImFont interMedium() {
        return ImFontManager.getInstance().get(INTER_MEDIUM);
    }

    /**
     * Get a map of all loaded fonts with their identifier.
     * @return An unmodifiable map of all fonts. Does not include ImGui's default font.
     */
    public Map<Identifier, ImFont> getFonts() {
        return ImFontManager.getInstance().getFonts();
    }

    /**
     * Get a font by its ID.
     * @param id ID to use.
     * @return The font, or a default font if it does not exist.
     */
    public ImFont getFont(Identifier id) {
        return ImFontManager.getInstance().get(id);
    }
}
