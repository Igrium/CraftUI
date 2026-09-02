package com.igrium.craftui.impl.render;

import cn.enaium.fabric.imgui.DefaultImGui;
import com.igrium.craftui.CraftUI;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import net.minecraft.util.Util;

/**
 * CraftUI's {@link cn.enaium.fabric.imgui.ImGuiService} implementation, registered via
 * {@code META-INF/services}. It reuses the library's context lifecycle, GLFW input, and
 * Blaze3D render backend, only customizing the IO configuration and exposing a font
 * re-upload hook.
 */
public class CraftImGuiService extends DefaultImGui {

    public CraftImGuiService() {
        // null id -> CraftUI manages layout persistence itself, so no per-mod .ini file.
        super(null);
    }

    @Override
    public void configure(ImGuiIO io) {
        super.configure(io); // default font + build atlas + setIniFilename(null) + DockingEnable
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        io.setConfigMacOSXBehaviors(Util.getPlatform() == Util.OS.OSX);
        if (CraftUI.getConfig().isEnableViewports()) {
            io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        }
    }

    public void reloadFontsTexture() {
        if (imGuiImplBlaze3D != null) {
            // Cleared here; newFrame() lazily rebuilds the pipeline + font texture from the new atlas.
            imGuiImplBlaze3D.dispose();
        }
        if (imGuiImplGl3 != null) {
            imGuiImplGl3.destroyFontsTexture();
            imGuiImplGl3.createFontsTexture();
        }
    }
}
