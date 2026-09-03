package com.igrium.craftui.api.nbt;

import imgui.ImGui;
import imgui.type.ImShort;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;

public final class NbtShortEditor extends NbtPrimitiveEditor<ShortTag> {
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
        return Tag.TAG_SHORT;
    }

    @Override
    public ShortTag getNbt() {
        return ShortTag.valueOf(value.get());
    }

    @Override
    public void setNbt(ShortTag nbt) {
        value.set(nbt.shortValue());
    }

    @Override
    protected Class<? extends ShortTag> getNbtClass() {
        return ShortTag.class;
    }
}
