package com.igrium.craftui.nbt;

import com.igrium.craftui.icon.NbtIcons;
import imgui.ImGui;
import imgui.type.ImString;
import lombok.NonNull;
import net.minecraft.nbt.*;
import net.minecraft.util.Language;

/**
 * A visual interface for editing NBT values.
 * @param <T> NBT element type
 * @apiNote For technical reasons, always deep-copies the NBT value on get/set
 */
public sealed abstract class NbtEditor<T extends NbtElement> permits NbtCompoundEditor, NbtListEditor, NbtPrimitiveEditor {

    /**
     * Construct a new NbtEditor of the correct type for a given NBT element, and use it as an initial value.
     * @param element The element to use
     * @return The new NBT editor, with the supplied element as its initial value.
     * @param <T> The type of element
     */
    @SuppressWarnings("unchecked")
    public static <T extends NbtElement> NbtEditor<T> of(@NonNull T element) {
        switch (element) {
            case NbtByte nByte -> {
                var editor = new NbtByteEditor();
                editor.setNbt(nByte);
                return (NbtEditor<T>) editor;
            }
            case NbtShort nShort -> {
                var editor = new NbtShortEditor();
                editor.setNbt(nShort);
                return (NbtEditor<T>) editor;
            }
            case NbtInt nInt -> {
                var editor = new NbtIntEditor();
                editor.setNbt(nInt);
                return (NbtEditor<T>) editor;
            }
            case NbtLong nLong -> {
                var editor = new NbtLongEditor();
                editor.setNbt(nLong);
                return (NbtEditor<T>) editor;
            }
            case NbtFloat nFloat -> {
                var editor = new NbtFloatEditor();
                editor.setNbt(nFloat);
                return (NbtEditor<T>) editor;
            }
            case NbtDouble nDouble -> {
                var editor = new NbtDoubleEditor();
                editor.setNbt(nDouble);
                return (NbtEditor<T>) editor;
            }
            case NbtCompound compound -> {
                var editor = new NbtCompoundEditor();
                editor.setNbt(compound);
                return (NbtEditor<T>) editor;
            }
            case NbtList list -> {
                var editor = new NbtListEditor();
                editor.setNbt(list);
                return (NbtEditor<T>) editor;
            }
            case NbtString string -> {
                var editor = new NbtStringEditor();
                editor.setNbt(string);
                return (NbtEditor<T>) editor;
            }
            default -> {
                return new NbtGenericEditor<>((T) element.copy());
            }
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
     *
     * @param id    ID to give to the underlying stack
     * @param label Label to render with
     * @param flags {@link NbtEditorFlags} to use
     * @return "return" flags
     */
    public abstract int render(String id, ImString label, int flags);

    protected abstract Class<? extends T> getNbtClass();

    protected abstract byte getNbtType();

    protected int drawContextItems(int flags) {
        if (ImGui.menuItem(t("gui.craftui.nbt.copyNbt"))) {
            var nbt = getNbt();
            ImGui.setClipboardText(nbt.asString());
        }
//        ImGui.beginDisabled(hasFlag(flags, NbtEditorFlags.READONLY));
//        if (ImGui.menuItem("gui.craftui.nbt.pasteNbt")) {
//            String snbt = ImGui.getClipboardText();
//        }
//        ImGui.endDisabled();
        return 0;
    }


    private final ImString labelStr = new ImString(32);

    /**
     * Force the label into edit mode if possible. Triggered next time it's rendered
     */
    protected void forceEditLabel() {
        // Default noop
    }
    /**
     * Draw this editor, handling right-click actions and other return flags automatically
     * @param label Label to render with
     * @param flags {@link NbtEditorFlags} to use
     * @return If the value was modified
     */
    public boolean render(String label, int flags) {
        labelStr.set(getRenderedText(label));
        flags |= NbtEditorFlags.READONLY_LABEL;
        String id = getId(label);

        int rFlags = render(id, labelStr, flags);

        if (hasFlag(rFlags, NbtEditorFlags.RETURN_RIGHT_CLICKED)) {
            ImGui.openPopup(id + ".context");
        }

        if (ImGui.beginPopup(id + ".context")) {
            drawContextItems(flags);
            ImGui.endPopup();
        }

        return hasFlag(rFlags, NbtEditorFlags.RETURN_MODIFIED);
    }


    private static String getRenderedText(String label) {
        int i = label.indexOf("##");
        return i == -1 ? label : label.substring(0, i);
    }

    private static String getId(String label) {
        int tripleHash = label.indexOf("###");
        return tripleHash >= 0 ? label.substring(tripleHash + 3) : label;
    }

    protected static boolean selectableText(String text) {
        return ImGui.selectable(text, ImGui.calcTextSizeX(text, true), ImGui.calcTextSizeY(text, true));
    }

    protected static byte drawTypeChooser() {
        for (byte i = 1; i <= 12; i++) {
            if (ImGui.menuItem(NbtIcons.getIcon(i) + " " + tt(NbtIcons.tooltipTranslation(i)))) {
                return i;
            }
        }
        return -1;
    }

    static boolean hasFlag(int flags, int flag) {
        return (flags & flag) != 0;
    }

    static String t(String key) {
        return Language.getInstance().get(key) + "###" + key;
    }

    static String tt(String key) {
        return Language.getInstance().get(key);
    }
}
