package com.igrium.craftui.impl.util;

import lombok.experimental.UtilityClass;
import net.minecraft.nbt.*;

@UtilityClass
public final class NbtTypes {

    public static NbtElement createElement(byte type) {
        return switch (type) {
            case NbtElement.END_TYPE -> NbtByte.of((byte) 0);
            case NbtElement.BYTE_TYPE -> NbtByte.of((byte) 0);
            case NbtElement.SHORT_TYPE -> NbtShort.of((short) 0);
            case NbtElement.INT_TYPE -> NbtInt.of(0);
            case NbtElement.LONG_TYPE -> NbtLong.of(0L);
            case NbtElement.FLOAT_TYPE -> NbtFloat.of(0f);
            case NbtElement.DOUBLE_TYPE -> NbtDouble.of(0d);
            case NbtElement.BYTE_ARRAY_TYPE -> new NbtByteArray(new byte[0]);
            case NbtElement.STRING_TYPE -> NbtString.of("");
            case NbtElement.LIST_TYPE -> new NbtList();
            case NbtElement.COMPOUND_TYPE -> new NbtCompound();
            case NbtElement.INT_ARRAY_TYPE -> new NbtIntArray(new int[0]);
            case NbtElement.LONG_ARRAY_TYPE -> new NbtLongArray(new long[0]);
            default -> throw new IllegalArgumentException("Unknown nbt type: " + type);
        };
    }

}
