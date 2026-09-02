package com.igrium.craftui.widgets.nbt;

import imgui.ImGui;
import imgui.type.ImString;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public final class NbtStringEditor extends NbtPrimitiveEditor<StringTag> {

    private ImString value = new ImString(32);
    private final EditableText editable = new EditableText();

    @Override
    protected boolean renderPrimitive(String id, int flags) {
        ImGui.alignTextToFramePadding();
        if ((flags & NbtEditorFlags.READONLY) != 0) {
            ImGui.text(value.get());
            return false;
        } else {
            return editable.editString(id, value, ImGui.getFontSize() * 16);
        }
    }

    @Override
    protected byte getNbtType() {
        return Tag.TAG_STRING;
    }

    @Override
    public StringTag getNbt() {
        return StringTag.valueOf(value.get());
    }

    @Override
    public void setNbt(StringTag nbt) {
        setValue(nbt.value());
    }

    @Override
    protected Class<? extends StringTag> getNbtClass() {
        return StringTag.class;
    }

    private void setValue(String value) {
        if (this.value.getBufferSize() < value.length()) {
            this.value = new ImString(value.length());
        }
        this.value.set(value);
    }
}
