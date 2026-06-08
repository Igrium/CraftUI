package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.type.ImDouble;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;

public final class NbtDoubleEditor extends NbtPrimitiveEditor<NbtDouble> {
    private final ImDouble value = new ImDouble();
    private final EditableText editable = new EditableText();

    @Override
    protected boolean renderPrimitive(String id, int flags) {
        ImGui.alignTextToFramePadding();
        if (NbtEditorFlags.isReadonly(flags)) {
            ImGui.text("" + value.get());
            return false;
        } else {
            return editable.editDouble(id, value, ImGui.getFontSize() * 8);
        }
    }

    @Override
    protected byte getNbtType() {
        return NbtElement.DOUBLE_TYPE;
    }

    @Override
    public NbtDouble getNbt() {
        return NbtDouble.of(value.get());
    }

    @Override
    public void setNbt(NbtDouble nbt) {
        value.set(nbt.doubleValue());
    }

    @Override
    protected Class<NbtDouble> getNbtClass() {
        return NbtDouble.class;
    }
}
