package com.igrium.craftui.nbt;

import imgui.type.ImString;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

/**
 * A visual interface for editing NBT values.
 * @param <T> NBT element type
 * @apiNote For technical reasons, always deep-copies the NBT value on get/set
 */
public sealed abstract class NbtEditor<T extends NbtElement> permits NbtCompoundEditor, NbtGenericEditor, NbtListEditor {

    /**
     * Construct a new NbtEditor of the correct type for a given NBT element, and use it as an initial value.
     * @param element The element to use
     * @return The new NBT editor, with the supplied element as its initial value.
     * @param <T> The type of element
     */
    @SuppressWarnings("unchecked")
    public static <T extends NbtElement> NbtEditor<T> of(T element) {
        if (element instanceof NbtCompound compound) {
            var editor = new NbtCompoundEditor();
            editor.setNbt(compound);
            return (NbtEditor<T>) editor;
        } else if (element instanceof NbtList list) {
            var editor = new NbtListEditor();
            editor.setNbt(list);
            return (NbtEditor<T>) editor;
        } else {
            var editor = new NbtGenericEditor<T>();
            editor.setNbt(element);
            return editor;
        }
    }
    
    /**
     * Get the editor's current value as NBT.
     * @return Editor's value as NBT.
     */
    public abstract T getNbt();

    /**
     * Load an NBT value into the editor.
     * @param nbt value to set
     */
    public abstract void setNbt(T nbt);

    /**
     * Draw this editor
     * @param id ID to give to the underlying stack
     * @param label Label to render with
     * @param flags {@link NbtEditorFlags} to use
     * @return If the value was updated this frame
     */
    public abstract boolean render(String id, ImString label, int flags);

    private final ImString labelStr = new ImString(32);

    public boolean render(String label, int flags) {
        labelStr.set(getRenderedText(label));
        return render(getId(label), labelStr, flags | NbtEditorFlags.READONLY_LABEL);
    }

    private static String getRenderedText(String label) {
        int i = label.indexOf("##");
        return i == -1 ? label : label.substring(0, i);
    }

    private static String getId(String label) {
        int tripleHash = label.indexOf("###");
        return tripleHash >= 0 ? label.substring(tripleHash + 3) : label;
    }
}
