package com.igrium.craftui.impl.file;

import com.igrium.craftui.app.AppManager;
import com.igrium.craftui.file.FileDialogInterface;
import com.igrium.craftui.file.FileDialogs;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ImFileDialog implements FileDialogInterface {
    @Override
    public void init() {

    }

    @Override
    public CompletableFuture<Optional<String>> showSaveDialog(@Nullable String defaultPath, @Nullable String defaultName, FileDialogs.FileFilter... filters) {
        ImFileDialogWidget widget = new ImFileDialogWidget();
        widget.setDefaultPath(defaultPath);

        AppManager.setGlobalPopup(widget::render);
        return widget.getFuture()
                .whenComplete((v, e) -> AppManager.setGlobalPopup(null));

    }

    @Override
    public CompletableFuture<Optional<String>> showOpenDialog(@Nullable String defaultPath, FileDialogs.FileFilter... filters) {
        ImFileDialogWidget widget = new ImFileDialogWidget();
        widget.setDefaultPath(defaultPath);

        AppManager.setGlobalPopup(widget::render);
        return widget.getFuture()
                .whenComplete((v, e) -> AppManager.setGlobalPopup(null));
    }

    @Override
    public CompletableFuture<Optional<String>> showOpenFolderDialog(@Nullable String defaultPath) {
        ImFileDialogWidget widget = new ImFileDialogWidget();
        widget.setDefaultPath(defaultPath);

        AppManager.setGlobalPopup(widget::render);
        return widget.getFuture()
                .whenComplete((v, e) -> AppManager.setGlobalPopup(null));
    }
}
