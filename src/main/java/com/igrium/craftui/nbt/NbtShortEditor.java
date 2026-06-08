package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.type.ImShort;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtShort;

public final class NbtShortEditor extends NbtPrimitiveEditor<NbtShort> {
    private final ImShort value = new ImShort();
    private final EditableText editable = new EditableText();

    @Override
    protected boolean renderPrimitive(String id, int flags) {
        ImGui.alignTextToFramePadding();
        if (NbtEditorFlags.isReadonly(flags)) {
            ImGui.text("" + value.get());
            return false;
        } else {
            return editable.editShort(id, value, ImGui.getFontSize() * 8);
        }
    }

    @Override
    protected byte getNbtType() {
        return NbtElement.SHORT_TYPE;
    }

    @Override
    public NbtShort getNbt() {
        return NbtShort.of(value.get());
    }

    @Override
    public void setNbt(NbtShort nbt) {
        value.set(nbt.shortValue());
    }

    @Override
    protected Class<? extends NbtShort> getNbtClass() {
        return NbtShort.class;
    }
}
