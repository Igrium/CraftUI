package com.igrium.craftui.nbt;

import imgui.ImGui;
import net.minecraft.nbt.NbtElement;

/**
 * A read-only editor that just display's the nbt's toString function
 */
public final class NbtGenericEditor<T extends NbtElement> extends NbtPrimitiveEditor<T> {

    private T element;

    public NbtGenericEditor(T element) {
        this.element = element;
    }

    @Override
    protected boolean renderPrimitive(String id, int flags) {
        ImGui.text(element.toString());
        return false;
    }

    @Override
    protected byte getNbtType() {
        return element.getType();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getNbt() {
        return (T) element.copy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setNbt(T nbt) {
        element = (T) nbt.copy();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Class<? extends T> getNbtClass() {
        return (Class<? extends T>) element.getClass();
    }


}
