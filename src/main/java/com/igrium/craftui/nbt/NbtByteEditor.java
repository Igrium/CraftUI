package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.flag.ImGuiDataType;
import imgui.type.ImShort;
import net.minecraft.nbt.NbtByte;

public class NbtByteEditor implements NbtElementEditor<NbtByte> {

    private final ImShort value = new ImShort(); // Not super happy about using short here, but whatever

    @Override
    public NbtByte getValue() {
        return NbtByte.of(value.byteValue());
    }

    @Override
    public void setValue(NbtByte value) {
        this.value.set(value.byteValue());
    }

    @Override
    public void renderEditor(String label) {
        ImGui.inputScalar(label, ImGuiDataType.S8, value);
    }
}
