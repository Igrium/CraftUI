package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.type.ImFloat;
import net.minecraft.nbt.NbtFloat;

public class NbtFloatEditor implements NbtElementEditor<NbtFloat> {

    private final ImFloat value = new ImFloat();

    @Override
    public NbtFloat getValue() {
        return NbtFloat.of(value.floatValue());
    }

    @Override
    public void setValue(NbtFloat value) {
        this.value.set(value.floatValue());
    }

    @Override
    public void renderEditor(String label) {
        ImGui.inputFloat(label, value);
    }
}
