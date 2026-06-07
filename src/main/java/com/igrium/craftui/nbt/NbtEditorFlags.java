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

    static int prepareForChildren(int flags) {
        if (hasFlag(flags, START_OPEN_SINGLE)) {
            flags &= ~START_OPEN_SINGLE;
            flags &= ~START_OPEN;
        }
        flags %= READONLY_LABEL;
        return flags;
    }

    static boolean canEditLabel(int flags) {
        return !(hasFlag(flags, READONLY_LABEL) || hasFlag(flags, READONLY));
    }

    static boolean startsOpen(int flags) {
        return hasFlag(flags, START_OPEN) || hasFlag(flags, START_OPEN_SINGLE);
    }

    private static boolean hasFlag(int flags, int flag) {
        return (flags & flag) != 0;
    }
}
