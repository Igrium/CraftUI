package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.flag.ImGuiDataType;
import imgui.type.ImShort;
import net.minecraft.nbt.NbtShort;

public class NbtShortEditor implements NbtElementEditor<NbtShort> {

    private final ImShort value = new ImShort();

    @Override
    public NbtShort getValue() {
        return NbtShort.of(value.get());
    }

    @Override
    public void setValue(NbtShort value) {
        this.value.set(value.shortValue());
    }

    @Override
    public void renderEditor(String label) {
        ImGui.inputScalar(label, ImGuiDataType.S16, value);
    }
}
