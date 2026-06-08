package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.type.ImInt;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtElement;

public final class NbtByteEditor extends NbtPrimitiveEditor<NbtByte> {
    private final ImInt value = new ImInt();
    private final EditableText editable = new EditableText();

    @Override
    protected boolean renderPrimitive(String id, int flags) {
        ImGui.alignTextToFramePadding();
        if (NbtEditorFlags.isReadonly(flags)) {
            ImGui.text("" + value.get());
            return false;
        } else {
            boolean modified = editable.editInt(id, value, ImGui.getFontSize() * 8);
            value.set(Math.clamp(value.get(), Byte.MIN_VALUE, Byte.MAX_VALUE));
            return modified;
        }
    }

    @Override
    protected byte getNbtType() {
        return NbtElement.BYTE_TYPE;
    }

    @Override
    public NbtByte getNbt() {
        return NbtByte.of((byte) value.get());
    }

    @Override
    public void setNbt(NbtByte nbt) {
        value.set(nbt.byteValue());
    }

    @Override
    protected Class<NbtByte> getNbtClass() {
        return NbtByte.class;
    }
}
