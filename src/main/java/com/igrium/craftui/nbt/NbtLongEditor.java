package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.type.ImLong;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtLong;

public final class NbtLongEditor extends NbtPrimitiveEditor<NbtLong> {
    private final ImLong value = new ImLong();
    private final EditableText editable = new EditableText();

    @Override
    protected boolean renderPrimitive(String id, int flags) {
        ImGui.alignTextToFramePadding();
        if (NbtEditorFlags.isReadonly(flags)) {
            ImGui.text("" + value.get());
            return false;
        } else {
            return editable.editLong(id, value, ImGui.getFontSize() * 8);
        }
    }

    @Override
    protected byte getNbtType() {
        return NbtElement.LONG_TYPE;
    }

    @Override
    public NbtLong getNbt() {
        return NbtLong.of(value.get());
    }

    @Override
    public void setNbt(NbtLong nbt) {
        value.set(nbt.longValue());
    }
}
