package com.igrium.craftui.impl.util;

import lombok.experimental.UtilityClass;
import net.minecraft.nbt.*;

@UtilityClass
public final class NbtTypes {

    public static Tag createElement(byte type) {
        return switch (type) {
            case Tag.TAG_END, Tag.TAG_BYTE -> ByteTag.valueOf((byte) 0);
            case Tag.TAG_SHORT -> ShortTag.valueOf((short) 0);
            case Tag.TAG_INT -> IntTag.valueOf(0);
            case Tag.TAG_LONG -> LongTag.valueOf(0L);
            case Tag.TAG_FLOAT -> FloatTag.valueOf(0f);
            case Tag.TAG_DOUBLE -> DoubleTag.valueOf(0d);
            case Tag.TAG_BYTE_ARRAY -> new ByteArrayTag(new byte[0]);
            case Tag.TAG_STRING -> StringTag.valueOf("");
            case Tag.TAG_LIST -> new ListTag();
            case Tag.TAG_COMPOUND -> new CompoundTag();
            case Tag.TAG_INT_ARRAY -> new IntArrayTag(new int[0]);
            case Tag.TAG_LONG_ARRAY -> new LongArrayTag(new long[0]);
            default -> throw new IllegalArgumentException("Unknown nbt type: " + type);
        };
    }

}
