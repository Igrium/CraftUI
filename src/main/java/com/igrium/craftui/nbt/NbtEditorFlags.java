package com.igrium.craftui.nbt;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class NbtEditorFlags {

    /**
     * Do not allow editing this element or its children
     */
    public static final int READONLY = 1;

    /**
     * Don't allow this element's label to be edited.
     * @apiNote Does <em>not</em> propagate to children
     */
    public static final int READONLY_LABEL = 2;

    /**
     * Don't render the icon for this element or its children
     */
    public static final int NO_ICON = 4;

    /**
     * If this element is a compound or a list, begin recursively open
     */
    public static final int START_OPEN = 8;

    /**
     * If this element is a compound or a list, begin open, but do not propagate to children
     */
    public static final int START_OPEN_SINGLE = 16;

    public static final int RETURN_MODIFIED = 1;
    public static final int RETURN_MODIFIED_LABEL = 2;
    public static final int RETURN_LEFT_CLICKED = 4;
    public static final int RETURN_RIGHT_CLICKED = 8;
    public static final int RETURN_ADDED_ITEM = 16;
    public static final int RETURN_REMOVED_ITEM = 32;
    public static final int RETURN_REARRANGED = 64;

    static int prepareForChildren(int flags) {
        if (hasFlag(flags, START_OPEN_SINGLE)) {
            flags &= ~START_OPEN_SINGLE;
            flags &= ~START_OPEN;
        }
        flags %= READONLY_LABEL;
        return flags;
    }

    static boolean canEditLabel(int flags) {
        return !hasFlag(flags, READONLY_LABEL) && !hasFlag(flags, READONLY);
    }

    static boolean startsOpen(int flags) {
        return hasFlag(flags, START_OPEN) || hasFlag(flags, START_OPEN_SINGLE);
    }

    static boolean isReadonly(int flags) {
        return hasFlag(flags, READONLY);
    }

    static boolean hasFlag(int flags, int flag) {
        return (flags & flag) != 0;
    }

    @Deprecated
    static int getReturnFlags(boolean modified, boolean modifiedLabel, boolean leftClicked, boolean rightClicked) {
        int rFlags = 0;
        if (modified) rFlags |= NbtEditorFlags.RETURN_MODIFIED;
        if (modifiedLabel) rFlags |= NbtEditorFlags.RETURN_MODIFIED_LABEL;
        if (leftClicked) rFlags |= NbtEditorFlags.RETURN_LEFT_CLICKED;
        if (rightClicked) rFlags |= NbtEditorFlags.RETURN_RIGHT_CLICKED;
        return rFlags;
    }
}
