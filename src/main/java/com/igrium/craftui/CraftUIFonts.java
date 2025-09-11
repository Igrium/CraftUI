package com.igrium.craftui;

import com.igrium.craftui.impl.font.ImFontManager;
import imgui.ImFont;
import lombok.experimental.UtilityClass;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * Interface to access standard CraftUI fonts.
 */
@UtilityClass
public final class CraftUIFonts {
    public static final Identifier INTER = Identifier.of("craftui:inter");
    public static ImFont inter() {
        return ImFontManager.getInstance().get(INTER);
    }

    public static final Identifier INTER_MEDIUM = Identifier.of("craftui:inter-medium");
    public static ImFont interMedium() {
        return ImFontManager.getInstance().get(INTER_MEDIUM);
    }

    public static final Identifier NBT_ICONS = Identifier.of("craftui:nbt-icons");
    public static ImFont nbtIcons() {
        return ImFontManager.getInstance().get(NBT_ICONS);
    }

    /**
     * Get a map of all loaded fonts with their identifier.
     * @return An unmodifiable map of all fonts. Does not include ImGui's default font.
     */
    public static Map<Identifier, ImFont> getFonts() {
        return ImFontManager.getInstance().getFonts();
    }

    /**
     * Get a font by its ID.
     * @param id ID to use.
     * @return The font, or a default font if it does not exist.
     */
    public static ImFont getFont(Identifier id) {
        return ImFontManager.getInstance().get(id);
    }
}
