package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.flag.ImGuiDataType;
import imgui.type.ImInt;
import net.minecraft.nbt.NbtInt;

public class NbtIntEditor implements NbtElementEditor<NbtInt> {

    private final ImInt value = new ImInt();

    @Override
    public NbtInt getValue() {
        return NbtInt.of(value.get());
    }

    @Override
    public void setValue(NbtInt value) {
        this.value.set(value.intValue());
    }

    @Override
    public void renderEditor(String label) {
        ImGui.inputScalar(label, ImGuiDataType.S32, value);
    }
}
