package com.igrium.craftui.nbt;

import net.minecraft.nbt.NbtElement;

public interface NbtElementEditor<T extends NbtElement> {
    T getValue();
    void setValue(T value);

    void renderEditor(String label);
}
