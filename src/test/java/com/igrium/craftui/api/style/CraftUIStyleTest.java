package com.igrium.craftui.api.style;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CraftUIStyleTest {

    /**
     * Fills every instance field with a distinct non-default value via reflection, so a field that is added
     * to {@link CraftUIStyle} but forgotten in {@link CraftUIStyle#copy()} makes this test fail.
     */
    @SuppressWarnings("unchecked")
    private static CraftUIStyle populatedStyle() throws IllegalAccessException {
        CraftUIStyle style = new CraftUIStyle();
        int n = 1;
        for (Field f : CraftUIStyle.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic())
                continue;
            f.setAccessible(true);
            Class<?> t = f.getType();
            n++;

            if (t == float.class) {
                f.setFloat(style, n + 0.5f);
            } else if (t == int.class) {
                f.setInt(style, n);
            } else if (t == boolean.class) {
                f.setBoolean(style, true);
            } else if (t == CraftUIStyle.StyleVec2.class) {
                f.set(style, new CraftUIStyle.StyleVec2(n, n + 0.25f));
            } else if (t == Identifier.class) {
                f.set(style, Identifier.fromNamespaceAndPath("craftui", "font_" + n));
            } else if (t == Map.class) {
                Map<String, CraftUIColors.UIColor> map = (Map<String, CraftUIColors.UIColor>) f.get(style);
                map.put("test_color_" + n, new CraftUIColors.UIColor(0xFF000000 | n, null));
            } else {
                fail("CraftUIStyleTest doesn't know how to populate field '" + f.getName()
                        + "' of type " + t.getName() + ". Update populatedStyle() and CraftUIStyle.copy().");
            }
        }
        return style;
    }

    @Test
    void copyCopiesEveryField() throws IllegalAccessException {
        CraftUIStyle original = populatedStyle();
        CraftUIStyle copy = original.copy();

        assertNotSame(original, copy);

        for (Field f : CraftUIStyle.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic())
                continue;
            f.setAccessible(true);
            assertEquals(f.get(original), f.get(copy), "Field '" + f.getName() + "' was not copied by copy()");
        }
    }

    @Test
    void copyDoesNotShareColorMap() throws IllegalAccessException {
        CraftUIStyle original = populatedStyle();
        CraftUIStyle copy = original.copy();

        Field colors;
        try {
            colors = CraftUIStyle.class.getDeclaredField("colors");
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
        colors.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, CraftUIColors.UIColor> copyColors = (Map<String, CraftUIColors.UIColor>) colors.get(copy);
        @SuppressWarnings("unchecked")
        Map<String, CraftUIColors.UIColor> originalColors = (Map<String, CraftUIColors.UIColor>) colors.get(original);
        int before = originalColors.size();

        assertNotSame(originalColors, copyColors);
        copyColors.put("added_to_copy_only", new CraftUIColors.UIColor(0xFFFFFFFF, null));
        assertEquals(before, originalColors.size());
    }

    @Test
    void mutatingCopyDoesNotAffectOriginal() throws IllegalAccessException {
        CraftUIStyle original = populatedStyle();
        float alpha = original.getAlpha();

        CraftUIStyle copy = original.copy();
        copy.setAlpha(alpha + 1);
        copy.setWindowPadding(new CraftUIStyle.StyleVec2(99, 99));

        assertEquals(alpha, original.getAlpha());
        assertNotEquals(new CraftUIStyle.StyleVec2(99, 99), original.getWindowPadding());
    }
}