package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.type.ImString;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;

public final class NbtStringEditor extends NbtPrimitiveEditor<NbtString> {

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
        return NbtElement.STRING_TYPE;
    }

    @Override
    public NbtString getNbt() {
        return NbtString.of(value.get());
    }

    @Override
    public void setNbt(NbtString nbt) {
        setValue(nbt.asString());
    }

    private void setValue(String value) {
        if (this.value.getBufferSize() < value.length()) {
            this.value = new ImString(value.length());
        }
        this.value.set(value);
    }
}
