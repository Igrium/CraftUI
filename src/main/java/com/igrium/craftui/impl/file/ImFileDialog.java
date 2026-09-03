package com.igrium.craftui.impl.file;

import com.igrium.craftui.api.CraftUI;
import com.igrium.craftui.api.file.FileDialogInterface;
import com.igrium.craftui.api.file.FileDialogs;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ImFileDialog implements FileDialogInterface {
    @Override
    public void init() {

    }

    @Override
    public CompletableFuture<Optional<String>> showSaveDialog(@Nullable String defaultPath, @Nullable String defaultName, FileDialogs.FileFilter... filters) {
        ImFileDialogPopup widget = new ImFileDialogPopup();
        widget.setDefaultPath(defaultPath);
        widget.setSaveMode(true);

        CraftUI.setGlobalPopup(widget::render);
        return widget.getFuture()
                .whenComplete((v, e) -> CraftUI.setGlobalPopup(null));

    }

    @Override
    public CompletableFuture<Optional<String>> showOpenDialog(@Nullable String defaultPath, FileDialogs.FileFilter... filters) {
        ImFileDialogPopup widget = new ImFileDialogPopup();
        widget.setDefaultPath(defaultPath);

        CraftUI.setGlobalPopup(widget::render);
        return widget.getFuture()
                .whenComplete((v, e) -> CraftUI.setGlobalPopup(null));
    }

    @Override
    public CompletableFuture<Optional<String>> showOpenFolderDialog(@Nullable String defaultPath) {
        ImFileDialogPopup widget = new ImFileDialogPopup();
        widget.setDefaultPath(defaultPath);
        widget.setDirMode(true);

        CraftUI.setGlobalPopup(widget::render);
        return widget.getFuture()
                .whenComplete((v, e) -> CraftUI.setGlobalPopup(null));
    }
}
