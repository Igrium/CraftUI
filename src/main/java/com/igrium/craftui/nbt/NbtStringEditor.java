package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.type.ImString;
import net.minecraft.nbt.NbtString;

public class NbtStringEditor implements NbtElementEditor<NbtString> {

    private final ImString value = new ImString(128);

    @Override
    public NbtString getValue() {
        return NbtString.of(value.get());
    }

    @Override
    public void setValue(NbtString value) {
        this.value.set(value);
    }

    @Override
    public void renderEditor(String label) {
        ImGui.inputText(label, value);
    }
}
