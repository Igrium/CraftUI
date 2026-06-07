package com.igrium.craftui.nbt;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

public abstract sealed class NbtEditor<T extends NbtElement> permits NbtCompoundEditor, NbtListEditor, NbtNullEditor, NbtStringEditor {

    @SuppressWarnings("unchecked")
    public static <T extends NbtElement> NbtEditor<T> of(T element) {
        switch (element) {
            case NbtCompound compound -> {
                var editor = new NbtCompoundEditor();
                editor.fromNbt(compound);
                return (NbtEditor<T>) editor;

            }
            case NbtList list -> {
                var editor = new NbtListEditor();
                editor.fromNbt(list);
                return (NbtEditor<T>) editor;

            }
            case NbtString string -> {
                var editor = new NbtStringEditor();
                editor.fromNbt(string);
                return (NbtEditor<T>) editor;
            }
            default -> {
                NbtNullEditor<T> editor = new NbtNullEditor<T>((Class<T>) element.getClass());
                editor.fromNbt(element);
                return editor;
            }
        }
    }

    /**
     * Load the values from an NBT element into the editor.
     * @param nbt NBT element to load.
     */
    public abstract void fromNbt(T nbt);

    /**
     * Save the values from the editor into an NBT element.
     * @return T savedNbt
     */
    public abstract T toNbt();

    /**
     * Render the editor with its current contents.
     * @param label The label to render with
     * @param flags Render flags
     */
    public abstract void render(String label, int flags);

    public abstract char getIcon();
}
