package com.igrium.craftui.impl.render;

import cn.enaium.fabric.imgui.DefaultImGui;
import com.igrium.craftui.impl.CraftUIEntrypoint;
import com.igrium.craftui.impl.mixin.AccessorDefaultImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import net.minecraft.util.Util;

/**
 * CraftUIEntrypoint's {@link cn.enaium.fabric.imgui.ImGuiService} implementation, registered via
 * {@code META-INF/services}. It reuses the library's context lifecycle, GLFW input, and
 * Blaze3D render backend, only customizing the IO configuration and exposing a font
 * re-upload hook.
 */
public class CraftImGuiService extends DefaultImGui {

    public CraftImGuiService() {
        // null id -> CraftUIEntrypoint manages layout persistence itself, so no per-mod .ini file.
        super(null);
    }

    @Override
    public void configure(ImGuiIO io) {
        super.configure(io); // default font + build atlas + setIniFilename(null) + DockingEnable
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        io.setConfigMacOSXBehaviors(Util.getPlatform() == Util.OS.OSX);
        if (CraftUIEntrypoint.getConfig().isEnableViewports()) {
            io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        }
    }

    public void reloadFontsTexture() {
        var implBlaze3d = ((AccessorDefaultImGui) this).getImGuiImplBlaze3D();
        if (implBlaze3d != null) {
            // Cleared here; newFrame() lazily rebuilds the pipeline + font texture from the new atlas.
            implBlaze3d.dispose();
        }
        var implGl3 = ((AccessorDefaultImGui) this).getImGuiImplGl3();
        if (implGl3 != null) {
            implGl3.destroyFontsTexture();
            implGl3.createFontsTexture();
        }
    }
}
