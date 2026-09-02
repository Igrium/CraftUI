package com.igrium.craftui.widgets.nbt;

import imgui.ImGui;
import imgui.type.ImLong;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;

public final class NbtLongEditor extends NbtPrimitiveEditor<LongTag> {
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
        return Tag.TAG_LONG;
    }

    @Override
    public LongTag getNbt() {
        return LongTag.valueOf(value.get());
    }

    @Override
    public void setNbt(LongTag nbt) {
        value.set(nbt.longValue());
    }

    @Override
    protected Class<? extends LongTag> getNbtClass() {
        return LongTag.class;
    }
}
