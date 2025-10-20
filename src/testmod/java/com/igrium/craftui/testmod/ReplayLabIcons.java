package com.igrium.craftui.testmod;

import com.igrium.craftui.CraftUIFonts;
import imgui.ImFont;
import lombok.experimental.UtilityClass;
import net.minecraft.util.Identifier;

@UtilityClass
public class ReplayLabIcons {
    public static final Identifier FONT_ID = Identifier.of("craftui-test:replaylab-icons");

    public static ImFont getFont() {
        return CraftUIFonts.getFont(FONT_ID);
    }

    public static final char ICON_VIDEOCAM = 0xe800;
    public static final char ICON_LOCK = 0xe801;
    public static final char ICON_LOCK_OPEN = 0xe802;
    public static final char ICON_PLAY = 0xe803;
    public static final char ICON_STOP = 0xe804;
    public static final char ICON_PAUSE = 0xe805;
    public static final char ICON_TO_END = 0xe806;
    public static final char ICON_TO_START = 0xe807;
    public static final char ICON_TO_START_ALT = 0xe808;
    public static final char ICON_FAST_FW = 0xe809;
    public static final char ICON_FAST_BW = 0xe80a;
    public static final char ICON_TO_END_ALT = 0xe80b;
    public static final char ICON_RESIZE_SMALL = 0xe80c;
    public static final char ICON_RESIZE_FULL = 0xe80d;
    public static final char ICON_MAGNET = 0xe80e;
    public static final char ICON_EYEDROPPER = 0xf1fb;

}
