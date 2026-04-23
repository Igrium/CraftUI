package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.type.ImDouble;
import net.minecraft.nbt.NbtDouble;

public class NbtDoubleEditor implements NbtElementEditor<NbtDouble> {

    private final ImDouble value = new ImDouble();

    @Override
    public NbtDouble getValue() {
        return NbtDouble.of(value.doubleValue());
    }

    @Override
    public void setValue(NbtDouble value) {
        this.value.set(value.doubleValue());
    }

    @Override
    public void renderEditor(String label) {
        ImGui.inputDouble(label, value);
    }
}
