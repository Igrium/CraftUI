package com.igrium.craftui.icon;

import com.igrium.craftui.CraftUIFonts;
import imgui.ImGui;
import lombok.experimental.UtilityClass;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;

@UtilityClass
public final class NbtIcons {
    private static final char OFFSET = 'A';

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
        ImGui.pushFont(CraftUIFonts.nbtIcons());
        ImGui.text(String.valueOf(getIcon(elementType)));
        ImGui.popFont();

        if (ImGui.isItemHovered()) {
            ImGui.setTooltip(Text.translatable(tooltipTranslation(elementType)).getString());
        }
    }

    private static String tooltipTranslation(byte elementType) {
        String suffix;
        switch (elementType) {
            case NbtElement.BYTE_TYPE -> suffix = "nbtByte";
            case NbtElement.SHORT_TYPE -> suffix = "nbtShort";
            case NbtElement.INT_TYPE -> suffix = "nbtInt";
            case NbtElement.LONG_TYPE -> suffix = "nbtLong";
            case NbtElement.FLOAT_TYPE -> suffix = "nbtFloat";
            case NbtElement.DOUBLE_TYPE -> suffix = "nbtDouble";
            case NbtElement.BYTE_ARRAY_TYPE -> suffix = "nbtByteArray";
            case NbtElement.STRING_TYPE -> suffix = "nbtString";
            case NbtElement.LIST_TYPE -> suffix = "nbtList";
            case NbtElement.COMPOUND_TYPE -> suffix = "nbtCompound";
            case NbtElement.INT_ARRAY_TYPE -> suffix = "nbtIntArray";
            case NbtElement.LONG_ARRAY_TYPE -> suffix = "nbtLongArray";
            default -> throw new IllegalArgumentException("Invalid NBT tag type: " + elementType);
        }

        return "tooltip.craftui." + suffix;
    }
}
