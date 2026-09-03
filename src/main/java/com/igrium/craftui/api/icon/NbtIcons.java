package com.igrium.craftui.api.icon;

import com.igrium.craftui.api.style.CraftUIFonts;
import imgui.ImGui;
import lombok.experimental.UtilityClass;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

@UtilityClass
public final class NbtIcons {
    private static final char OFFSET = 0xEAFB;

    public static final char ICON_BYTE = OFFSET + Tag.TAG_BYTE;
    public static final char ICON_SHORT = OFFSET + Tag.TAG_SHORT;
    public static final char ICON_INT = OFFSET + Tag.TAG_INT;
    public static final char ICON_LONG = OFFSET + Tag.TAG_LONG;
    public static final char ICON_FLOAT = OFFSET + Tag.TAG_FLOAT;
    public static final char ICON_DOUBLE = OFFSET + Tag.TAG_DOUBLE;
    public static final char ICON_BYTE_ARRAY = OFFSET + Tag.TAG_BYTE_ARRAY;
    public static final char ICON_STRING = OFFSET + Tag.TAG_STRING;
    public static final char ICON_LIST = OFFSET + Tag.TAG_LIST;
    public static final char ICON_COMPOUND = OFFSET + Tag.TAG_COMPOUND;
    public static final char ICON_INT_ARRAY = OFFSET + Tag.TAG_INT_ARRAY;
    public static final char ICON_LONG_ARRAY = OFFSET + Tag.TAG_LONG_ARRAY;

    /**
     * Return the ASCII character for a given NBT element type.
     * @param elementType Element type as defined by the NBT format.
     * @return Character representing that type on the icon font.
     */
    public static char getIcon(byte elementType) {
        return (char) (OFFSET + elementType);
    }

    /**
     * Draw the icon for a given NBt element type. Requires an active IMGUI context.
     *
     * @param elementType Element type as defined by the NBT format.
     * @apiNote It is recommended to call <code>ImGui.alignTextToFramePadding()</code>
     * on any line where an icon is used.
     */
    public static void drawIcon(byte elementType) {
        ImGui.pushFont(CraftUIFonts.nbtIcons(), 0);
        ImGui.text("" + getIcon(elementType));
        ImGui.popFont();

        ImGui.setItemTooltip(Component.translatable(translation(elementType)).getString());
    }

    /**
     * Get only the suffix of the translation key for a given NBT element type
     * @param elementType NBT element type to get
     * @return Translation suffix
     */
    public static String translationSuffix(byte elementType) {
        return switch (elementType) {
            case Tag.TAG_BYTE -> "nbtByte";
            case Tag.TAG_SHORT -> "nbtShort";
            case Tag.TAG_INT -> "nbtInt";
            case Tag.TAG_LONG -> "nbtLong";
            case Tag.TAG_FLOAT -> "nbtFloat";
            case Tag.TAG_DOUBLE -> "nbtDouble";
            case Tag.TAG_BYTE_ARRAY -> "nbtByteArray";
            case Tag.TAG_STRING -> "nbtString";
            case Tag.TAG_LIST -> "nbtList";
            case Tag.TAG_COMPOUND -> "nbtCompound";
            case Tag.TAG_INT_ARRAY -> "nbtIntArray";
            case Tag.TAG_LONG_ARRAY -> "nbtLongArray";
            default -> throw new IllegalArgumentException("Invalid NBT tag type: " + elementType);
        };
    }

    /**
     * Get the translation key for a given NBT element type
     * @param elementType NBT element type to get
     * @return Translation key
     */
    public static String translation(byte elementType) {
        return "tooltip.craftui." + translationSuffix(elementType);
    }
}
