package com.igrium.craftui.nbt;

import imgui.ImGui;
import net.minecraft.nbt.NbtIntArray;

public class NbtIntArrayEditor implements NbtElementEditor<NbtIntArray> {

    private NbtIntArray value = new NbtIntArray(new int[0]);

    @Override
    public NbtIntArray getValue() {
        return value.copy();
    }

    @Override
    public void setValue(NbtIntArray value) {
        this.value = value.copy();
    }

    @Override
    public void renderEditor(String label) {
        ImGui.labelText(label, "[int array not implemented]");
    }
}
