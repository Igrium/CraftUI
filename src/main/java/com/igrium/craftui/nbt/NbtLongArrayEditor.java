package com.igrium.craftui.nbt;

import imgui.ImGui;
import net.minecraft.nbt.NbtLongArray;

public class NbtLongArrayEditor implements NbtElementEditor<NbtLongArray> {

    private NbtLongArray value = new NbtLongArray(new long[0]);

    @Override
    public NbtLongArray getValue() {
        return value.copy();
    }

    @Override
    public void setValue(NbtLongArray value) {
        this.value = value.copy();
    }

    @Override
    public void renderEditor(String label) {
        ImGui.labelText(label, "[long array not implemented]");
    }
}
