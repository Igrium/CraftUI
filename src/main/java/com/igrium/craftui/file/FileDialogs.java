package com.igrium.craftui.file;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileDialogs {
    public static record FileFilter(String name, String... extensions) {
    }

    private static FileDialogInterface impl;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDialogs.class);

    private static synchronized boolean init() {
        if (impl == null) {
            try {
                impl = new NFDFileDialog();
                impl.init();
                return true;
            } catch (Throwable e) {
                LOGGER.error("Error initializing NFD. Falling back to AWT", e);
            }
            try {
                impl = new AWTFileDialog();
                impl.init();
                return true;
            } catch (Throwable t) {
                LOGGER.error("Error initializing AWT file dialog. Falling back to ImGUI approach.", t);
            }
            try {
                impl = new ImFileDialog();
                impl.init();
                return true;
            } catch (Throwable t) {
                LOGGER.error("Error initializing file dialogs", t);
            }
            return false;
        }

        return true;
    }

    public static CompletableFuture<Optional<String>> saveDialog(@Nullable String defaultPath,
            @Nullable String defaultName, FileFilter... filters) {
        if (init()) {
            return impl.saveDialog(defaultPath, defaultName, filters).exceptionally(FileDialogs::handle);
        } else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    public static CompletableFuture<Optional<String>> openDialog(@Nullable String defaultPath, FileFilter... filters) {
        if (init()) {
            return impl.openDialog(defaultPath, filters).exceptionally(FileDialogs::handle);
        } else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    public static CompletableFuture<Optional<String>> pickFolder(@Nullable String defaultPath) {
        if (init()) {
            return impl.pickFolder(defaultPath).exceptionally(FileDialogs::handle);
        } else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    private static Optional<String> handle(Throwable e) {
        LOGGER.error("Error opening file dialog", e);
        return Optional.empty();
    }
}
