package com.igrium.craftui.widgets.nbt;

import imgui.ImGui;
import net.minecraft.nbt.Tag;
import org.jspecify.annotations.NonNull;

/**
 * A read-only editor that just display's the nbt's toString function
 */
public final class NbtGenericEditor<T extends Tag> extends NbtPrimitiveEditor<T> {

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
        return element.getId();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getNbt() {
        return (T) element.copy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setNbt(@NonNull T nbt) {
        element = (T) nbt.copy();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Class<T> getNbtClass() {
        return (Class<T>) element.getClass();
    }


}
