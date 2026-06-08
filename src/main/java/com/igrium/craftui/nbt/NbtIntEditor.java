package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.type.ImInt;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;

public final class NbtIntEditor extends NbtPrimitiveEditor<NbtInt> {
    private final ImInt value = new ImInt();
    private final EditableText editable = new EditableText();

    @Override
    protected boolean renderPrimitive(String id, int flags) {
        ImGui.alignTextToFramePadding();
        if (NbtEditorFlags.isReadonly(flags)) {
            ImGui.text("" + value.get());
            return false;
        } else {
            return editable.editInt(id, value, ImGui.getFontSize() * 8);
        }
    }

    @Override
    protected byte getNbtType() {
        return NbtElement.INT_TYPE;
    }

    @Override
    public NbtInt getNbt() {
        return NbtInt.of(value.get());
    }

    @Override
    public void setNbt(NbtInt nbt) {
        value.set(nbt.intValue());
    }

    @Override
    protected Class<? extends NbtInt> getNbtClass() {
        return NbtInt.class;
    }
}
