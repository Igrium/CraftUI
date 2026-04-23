package com.igrium.craftui.nbt;

import com.igrium.craftui.impl.util.NbtEditor;
import imgui.ImGui;
import net.minecraft.nbt.NbtByteArray;

public class NbtByteArrayEditor implements NbtElementEditor<NbtByteArray> {

    private NbtByteArray value = new NbtByteArray(new byte[0]);

    @Override
    public NbtByteArray getValue() {
        return (NbtByteArray) value.copy();
    }

    @Override
    public void setValue(NbtByteArray value) {
        this.value = (NbtByteArray) value.copy();
    }

    @Override
    public void renderEditor(String label) {
        ImGui.labelText(label, "[byte array not implemented]");
    }
}
