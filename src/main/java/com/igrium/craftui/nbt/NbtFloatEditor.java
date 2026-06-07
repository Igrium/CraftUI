package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.type.ImFloat;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;

public final class NbtFloatEditor extends NbtPrimitiveEditor<NbtFloat> {
    private final ImFloat value = new ImFloat();
    private final EditableText editable = new EditableText();

    @Override
    protected boolean renderPrimitive(String id, int flags) {
        ImGui.alignTextToFramePadding();
        if (NbtEditorFlags.isReadonly(flags)) {
            ImGui.text("" + value.get());
            return false;
        } else {
            return editable.editFloat(id, value, ImGui.getFontSize() * 8);
        }
    }

    @Override
    protected byte getNbtType() {
        return NbtElement.FLOAT_TYPE;
    }

    @Override
    public NbtFloat getNbt() {
        return NbtFloat.of(value.get());
    }

    @Override
    public void setNbt(NbtFloat nbt) {
        value.set(nbt.floatValue());
    }
}
