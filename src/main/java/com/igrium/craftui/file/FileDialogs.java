package com.igrium.craftui.file;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.craftui.CraftUI;

public class FileDialogs {
    public static record FileFilter(String name, String... extensions) {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDialogs.class);


    private static boolean isPreferNative() {
        return CraftUI.getConfig().isPreferNativeFileDialog();
    }


    private static NFDFileDialog nfdImpl;
    private static ImFileDialog imImpl;

    private static FileDialogInterface impl;

    private static synchronized void init() {
        if (impl == null) {
            if (!(isPreferNative() && initNfd())) {
                initInternal();
            }
        }
    }

    private static boolean initNfd() {
        if (nfdImpl == null) {
            try {
                NFDFileDialog newImpl = new NFDFileDialog();
                newImpl.init();
                nfdImpl = newImpl;
            } catch (Throwable e) {
                LOGGER.error("Error initializing NFD. Falling back to internal.", e);
                return false;
            }
        }
        impl = nfdImpl;
        return true;
    }

    private static void initInternal() {
        if (imImpl == null) {
            imImpl = new ImFileDialog();
            imImpl.init();
        }
        impl = imImpl;
    }

    public static void clearImpl() {
        impl = null;
    }

    public static CompletableFuture<Optional<String>> saveDialog(@Nullable String defaultPath,
            @Nullable String defaultName, FileFilter... filters) {
        init();
        return impl.saveDialog(defaultPath, defaultName, filters).exceptionally(FileDialogs::handle);
    }

    public static CompletableFuture<Optional<String>> openDialog(@Nullable String defaultPath, FileFilter... filters) {
        init();
        return impl.openDialog(defaultPath, filters).exceptionally(FileDialogs::handle);
    }

    public static CompletableFuture<Optional<String>> pickFolder(@Nullable String defaultPath) {
        init();
        return impl.pickFolder(defaultPath).exceptionally(FileDialogs::handle);
    }

    private static Optional<String> handle(Throwable e) {
        LOGGER.error("Error opening file dialog", e);
        return Optional.empty();
    }
}
