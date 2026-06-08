package com.igrium.craftui.icon;

import com.igrium.craftui.CraftUIFonts;
import imgui.ImGui;
import lombok.experimental.UtilityClass;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;

@UtilityClass
public final class NbtIcons {
    private static final char OFFSET = 0xEAFB;

    public static final char ICON_BYTE = OFFSET + NbtElement.BYTE_TYPE;
    public static final char ICON_SHORT = OFFSET + NbtElement.SHORT_TYPE;
    public static final char ICON_INT = OFFSET + NbtElement.INT_TYPE;
    public static final char ICON_LONG = OFFSET + NbtElement.LONG_TYPE;
    public static final char ICON_FLOAT = OFFSET + NbtElement.FLOAT_TYPE;
    public static final char ICON_DOUBLE = OFFSET + NbtElement.DOUBLE_TYPE;
    public static final char ICON_BYTE_ARRAY = OFFSET + NbtElement.BYTE_ARRAY_TYPE;
    public static final char ICON_STRING = OFFSET + NbtElement.STRING_TYPE;
    public static final char ICON_LIST = OFFSET + NbtElement.LIST_TYPE;
    public static final char ICON_COMPOUND = OFFSET + NbtElement.COMPOUND_TYPE;
    public static final char ICON_INT_ARRAY = OFFSET + NbtElement.INT_ARRAY_TYPE;
    public static final char ICON_LONG_ARRAY = OFFSET + NbtElement.LONG_ARRAY_TYPE;

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
        ImGui.pushFont(CraftUIFonts.nbtIcons(), ImGui.getFontSize());
        ImGui.text("" + getIcon(elementType));
        ImGui.popFont();

        ImGui.setItemTooltip(Text.translatable(translation(elementType)).getString());
    }

    /**
     * Get only the suffix of the translation key for a given NBT element type
     * @param elementType NBT element type to get
     * @return Translation suffix
     */
    public static String translationSuffix(byte elementType) {
        return switch (elementType) {
            case NbtElement.BYTE_TYPE -> "nbtByte";
            case NbtElement.SHORT_TYPE -> "nbtShort";
            case NbtElement.INT_TYPE -> "nbtInt";
            case NbtElement.LONG_TYPE -> "nbtLong";
            case NbtElement.FLOAT_TYPE -> "nbtFloat";
            case NbtElement.DOUBLE_TYPE -> "nbtDouble";
            case NbtElement.BYTE_ARRAY_TYPE -> "nbtByteArray";
            case NbtElement.STRING_TYPE -> "nbtString";
            case NbtElement.LIST_TYPE -> "nbtList";
            case NbtElement.COMPOUND_TYPE -> "nbtCompound";
            case NbtElement.INT_ARRAY_TYPE -> "nbtIntArray";
            case NbtElement.LONG_ARRAY_TYPE -> "nbtLongArray";
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
