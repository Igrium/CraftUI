package com.igrium.craftui.file;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import com.igrium.craftui.app.AppManager;
import com.igrium.craftui.file.FileDialogs.FileFilter;

class ImFileDialog implements FileDialogInterface {

    @Override
    public void init() {

    }

    @Override
    public CompletableFuture<Optional<String>> showSaveDialog(@Nullable String defaultPath, @Nullable String defaultName,
                                                              FileFilter... filters) {
        ImFileDialogApp app = new ImFileDialogApp();

        if (defaultPath != null)
            app.setDefaultPath(defaultPath);
        if (defaultName != null)
            app.setDefaultFilename(defaultName);

        if (filters != null && filters.length != 0) {
            app.setFilters(compileFilters(filters));
        } else if (defaultName != null) {
            String ext = FilenameUtils.getExtension(defaultName);
            if (!ext.isBlank()) {
                app.setFilters("." + ext);
            }
        }

        app.setTitle("Save As");
        app.setWarnOnOverride(true);

        var future = app.getFuture();
        AppManager.openApp(app);
        future.whenComplete((opt, t) -> AppManager.closeApp(app));
        return future;
    }

    @Override
    public CompletableFuture<Optional<String>> showOpenDialog(@Nullable String defaultPath, FileFilter... filters) {
        ImFileDialogApp app = new ImFileDialogApp();

        if (defaultPath != null)
            app.setDefaultPath(defaultPath);

        if (filters != null && filters.length != 0) {
            app.setFilters(compileFilters(filters));
        }

        var future = app.getFuture();
        AppManager.openApp(app);
        future.whenComplete((opt, t) -> AppManager.closeApp(app));
        return future;
    }

    @Override
    public CompletableFuture<Optional<String>> showOpenFolderDialog(@Nullable String defaultPath) {
        ImFileDialogApp app = new ImFileDialogApp();
        if (defaultPath != null)
            app.setDefaultPath(defaultPath);
        
        app.setFilters(null);
        app.setTitle("Select Folder");

        var future = app.getFuture();
        AppManager.openApp(app);
        future.whenComplete((opt, t) -> AppManager.closeApp(app));
        return future;
    }
    
    private static String compileFilters(FileFilter... filters) {
        List<String> strings = Arrays.stream(filters)
                .flatMap(f -> Arrays.stream(f.extensions()))
                .map(f -> f.startsWith(".") ? f : "." + f).toList();
        return String.join(",", strings);
    }
}
