package com.igrium.craftui.nbt;

import imgui.ImGui;
import net.minecraft.nbt.NbtElement;

import java.lang.reflect.InvocationTargetException;

public final class NbtNullEditor<T extends NbtElement> extends NbtEditor<T> {
    private final Class<T> elementClass;
    private T currentVal;

    public NbtNullEditor(Class<T> elementClass) {
        this.elementClass = elementClass;
        try {
            currentVal = elementClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void fromNbt(T nbt) {
        currentVal = elementClass.cast(nbt.copy());
    }

    @Override
    public T toNbt() {
        return currentVal;
    }

    @Override
    public void render(String label, int flags) {
        ImGui.text(label + ": " + currentVal);
    }

    @Override
    public char getIcon() {
        return '?';
    }
}
