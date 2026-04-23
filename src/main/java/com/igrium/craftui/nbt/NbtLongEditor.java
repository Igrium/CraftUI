package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.flag.ImGuiDataType;
import imgui.type.ImLong;
import net.minecraft.nbt.NbtLong;

public class NbtLongEditor implements NbtElementEditor<NbtLong> {

    private final ImLong value = new ImLong();

    @Override
    public NbtLong getValue() {
        return NbtLong.of(value.longValue());
    }

    @Override
    public void setValue(NbtLong value) {
        this.value.set(value.longValue());
    }

    @Override
    public void renderEditor(String label) {
        ImGui.inputScalar(label, ImGuiDataType.S64, value);
    }
}
