package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.type.ImDouble;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.Tag;

public final class NbtDoubleEditor extends NbtPrimitiveEditor<DoubleTag> {
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
        return Tag.TAG_DOUBLE;
    }

    @Override
    public DoubleTag getNbt() {
        return DoubleTag.valueOf(value.get());
    }

    @Override
    public void setNbt(DoubleTag nbt) {
        value.set(nbt.doubleValue());
    }

    @Override
    protected Class<DoubleTag> getNbtClass() {
        return DoubleTag.class;
    }
}
