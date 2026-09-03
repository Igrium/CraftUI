package com.igrium.craftui.api.nbt;

import imgui.ImGui;
import imgui.type.ImFloat;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.Tag;

public final class NbtFloatEditor extends NbtPrimitiveEditor<FloatTag> {
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
        return Tag.TAG_FLOAT;
    }

    @Override
    public FloatTag getNbt() {
        return FloatTag.valueOf(value.get());
    }

    @Override
    public void setNbt(FloatTag nbt) {
        value.set(nbt.floatValue());
    }

    @Override
    protected Class<FloatTag> getNbtClass() {
        return FloatTag.class;
    }
}
