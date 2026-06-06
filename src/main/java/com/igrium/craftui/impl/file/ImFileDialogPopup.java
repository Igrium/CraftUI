package com.igrium.craftui.impl.file;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Language;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class ImFileDialogPopup {

    @Getter
    private final CompletableFuture<Optional<String>> future = new CompletableFuture<>();

    @Getter @Setter
    private String defaultPath = ".";

    @Getter @Setter
    private boolean saveMode;

    @Getter @Setter
    private boolean dirMode;

    private final ImBoolean open = new ImBoolean(false);
    private final ImString filepath = new ImString();

    private final ImFileDialogWidget widget;
    private boolean firstRender = true;

    public ImFileDialogPopup() {
        widget = new ImFileDialogWidget();
    }

    public void render() {

        String title;
        if (saveMode) {
            title = "gui.craftui.fd_saveAs";
        } else {
            title  = dirMode ? "gui.craftui.fd_selectFolder" : "gui.craftui.fd_selectFile";
        }

        String id = Language.getInstance().get(title) + "###fileDialog";;

        if (firstRender) {
            firstRender = false;

            ImGui.openPopup(id);
            widget.setSaveMode(saveMode);
            widget.setDirMode(dirMode);
            if (defaultPath != null)
                widget.setPath(Paths.get(defaultPath));

            open.set(true);
        }

        float txtSize = ImGui.getFontSize();
        ImGui.setNextWindowSize(txtSize * 53, txtSize * 40, ImGuiCond.FirstUseEver);
        if (ImGui.beginPopupModal(id, open, ImGuiWindowFlags.NoSavedSettings)) {
            widget.render();

            if (ImGui.shortcut(ImGuiKey.Escape)) {
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }

        if (!open.get() || !widget.isOpen()) {
            open.set(false);
            Path outPath = widget.getOutPath();
            Optional<String> result = outPath != null ? Optional.of(outPath.toString()) : Optional.empty();
            future.complete(result);
        }
    }

    private static String t(String key) {
        return Language.getInstance().get(key);
    }
}
