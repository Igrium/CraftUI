package com.igrium.craftui.impl.config;

import com.igrium.craftui.CraftUI;
import com.igrium.craftui.app.CraftApp;
import com.igrium.craftui.file.FileDialogs;
import com.igrium.craftui.screen.CraftAppScreen;
import com.igrium.craftui.style.CraftUILayouts;
import com.igrium.craftui.style.CraftUIStyles;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class CraftUIConfigApp extends CraftApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(CraftUIConfigApp.class);

    /**
     * Mutable config instance.
     */
    private final CraftUIConfig config = CraftUI.getConfig();

    private final ImBoolean preferNativeFileDialog = new ImBoolean(config.isPreferNativeFileDialog());
    private final ImBoolean enableViewports = new ImBoolean(config.isEnableViewports());
    private final ImBoolean layoutPersistent = new ImBoolean(config.isLayoutPersistent());
    private final ImBoolean enableDebugCommand = new ImBoolean(config.isEnableDebugCommands());

    private Identifier[] styles;
    private final String[] styleNames;

    private final ImInt selectedStyle = new ImInt();

    public CraftUIConfigApp() {
        styles = CraftUIStyles.getStyles().keySet().toArray(new Identifier[0]);
        styleNames = Arrays.stream(styles)
                .map(i -> Language.getInstance().get(i.toTranslationKey("style")))
                .toArray(String[]::new);

        int styleIndex = find(CraftUIStyles.getActiveStyle(), styles);
        if (styleIndex >= 0) {
            selectedStyle.set(styleIndex);
        }
    }

    private static <T> int find(T value, T[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void render(MinecraftClient client) {
        boolean wantsSave = false;

        var viewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(viewport.getCenterX(), viewport.getCenterY(), ImGuiCond.Always, .5f, .5f);


        if (ImGui.begin(
                Text.translatable("options.craftui.header").getString(), ImGuiWindowFlags.NoCollapse
                        | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoSavedSettings)) {

            if (ImGui.getIO().getKeysDown(GLFW.GLFW_KEY_ESCAPE) && ImGui.isWindowFocused()) {
                close();
            }

            if (combo("options.craftui.style", selectedStyle, styleNames, "options.craftui.style.tooltip"))
                wantsSave = true;

            if (checkbox("options.craftui.preferNativeFileDialog", preferNativeFileDialog, "options.craftui.preferNativeFileDialog.tooltip"))
                wantsSave = true;

            if (checkbox("options.craftui.enableViewports", enableViewports, "options.craftui.enableViewports.tooltip"))
                wantsSave = true;

            if (checkbox("options.craftui.layoutPersistent", layoutPersistent, "options.craftui.layoutPersistent.tooltip"))
                wantsSave = true;

            if (checkbox("options.craftui.enableDebugCommands", enableDebugCommand, "options.craftui.enableDebugCommands.tooltip"))
                wantsSave = true;

            ImGui.separator();

            if (button("options.craftui.resetLayout", "options.craftui.resetLayout.tooltip")) {
                CraftUILayouts.resetLayouts();
            }

            ImGui.sameLine();

            if (button("options.craftui.testFileDialog", "options.craftui.testFileDialog.tooltip")) {
                FileDialogs.showOpenDialog(null).whenComplete((v, e) -> {
                    if (e != null) {
                        LOGGER.error("Error opening file dialog: ", e);
                    } else {
                        v.ifPresentOrElse(file -> {
                            LOGGER.info("You opened {}", file);
                        }, () -> {
                            LOGGER.info("You didn't choose a file.");
                        });
                    }
                });
            }

            ImGui.separator();

            if (button("gui.done", null)) {
                close();
            }
        }
        ImGui.end();

        if (wantsSave) {
            save();
        }
    }

    private boolean checkbox(String name, ImBoolean active, @Nullable String tooltip) {
        boolean updated = ImGui.checkbox(Language.getInstance().get(name), active);
        setTooltip(tooltip);
        return updated;
    }

    private boolean combo(String name, ImInt currentItem, String[] items, @Nullable String tooltip) {
        boolean updated = ImGui.combo(Language.getInstance().get(name), currentItem, items);
        setTooltip(tooltip);
        return updated;
    }

    private boolean button(String name, @Nullable String tooltip) {
        boolean pressed = ImGui.button(Language.getInstance().get(name));
        setTooltip(tooltip);
        return pressed;
    }


    private void setTooltip(@Nullable String tooltip) {
        if (tooltip != null && ImGui.isItemHovered(ImGuiHoveredFlags.AllowWhenDisabled)) {
            ImGui.setTooltip(Language.getInstance().get(tooltip));
        }
    }

    private void save() {
        config.setStyle(styles[selectedStyle.get()]);
        config.setPreferNativeFileDialog(preferNativeFileDialog.get());
        config.setEnableViewports(enableViewports.get());
        config.setLayoutPersistent(layoutPersistent.get());
        config.setEnableDebugCommands(enableDebugCommand.get());
        CraftUI.saveConfig();
    }

    public static CraftAppScreen<CraftUIConfigApp> createScreen() {
        return new CraftAppScreen<>(new CraftUIConfigApp());
    }
}
