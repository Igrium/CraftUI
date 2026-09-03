package com.igrium.craftui.api.nbt;

import imgui.ImGui;
import imgui.type.ImInt;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;

public final class NbtIntEditor extends NbtPrimitiveEditor<IntTag> {
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
        return Tag.TAG_INT;
    }

    @Override
    public IntTag getNbt() {
        return IntTag.valueOf(value.get());
    }

    @Override
    public void setNbt(IntTag nbt) {
        value.set(nbt.intValue());
    }

    @Override
    protected Class<IntTag> getNbtClass() {
        return IntTag.class;
    }
}
