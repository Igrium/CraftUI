package com.igrium.craftui.file;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.igrium.craftui.impl.config.CraftUIConfigCallback;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.craftui.CraftUI;

/**
 * Allows apps to open system file dialogs for loading & saving.
 */
public class FileDialogs {
    /**
     * A type of file that may be accepted by a file dialog.
     * @param name The name of the file type. ex: "JPEG File".
     * @param extensions The file extensions this filter supports. ex: ["jpg", "jpeg"]
     * @see javax.swing.filechooser.FileNameExtensionFilter
     */
    public record FileFilter(String name, String... extensions) {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDialogs.class);

    private static boolean isPreferNative() {
        return CraftUI.getConfig().isPreferNativeFileDialog();
    }

    private static NFDFileDialog nfdImpl;
    private static ImFileDialog imImpl;

    private static FileDialogInterface impl;

    private static boolean isFirstInit = true;

    private static synchronized void init() {
        if (impl == null) {
            if (!(isPreferNative() && initNfd())) {
                initImgui();
            }

            if (isFirstInit) {
                CraftUIConfigCallback.EVENT.register(config -> {
                    impl = null;
                });
                isFirstInit = true;
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

    private static void initImgui() {
        if (imImpl == null) {
            imImpl = new ImFileDialog();
            imImpl.init();
        }
        impl = imImpl;
    }

    /**
     * Next time we attempt to open a file dialog, re-query the options to see which we should use.
     */
    public static void clearImpl() {
        impl = null;
    }

    /**
     * Show the save file dialog.
     *
     * @param defaultPath Default folder to open to. If <code>null</code>, left to the discretion of the dialog implementation.
     * @param defaultName Default filename to save with. If <code>null</code>, left to the discretion of the dialog implementation.
     * @param filters     A list of file filters to use. If empty, all files are accepted.
     * @return A future that completes once the dialog has closed.
     */
    public static CompletableFuture<Optional<String>> showSaveDialog(@Nullable String defaultPath,
                                                                     @Nullable String defaultName, FileFilter... filters) {
        init();
        return impl.showSaveDialog(defaultPath, defaultName, filters).exceptionally(FileDialogs::handle);
    }

    /**
     * Show the open file dialog.
     *
     * @param defaultPath Default folder to open to. If <code>null</code>, left to the discretion of the dialog implementation.
     * @param filters     A list of file filters to use. If empty, all files are accepted.
     * @return A future that completes once the dialog has closed.
     */
    public static CompletableFuture<Optional<String>> showOpenDialog(@Nullable String defaultPath, FileFilter... filters) {
        init();
        return impl.showOpenDialog(defaultPath, filters).exceptionally(FileDialogs::handle);
    }

    /**
     * Show the open folder dialog.
     *
     * @param defaultPath Default folder to open to. If <code>null</code>, left to the discretion of the dialog implementation.
     * @return a future that completes once the dialog has closed.
     */
    public static CompletableFuture<Optional<String>> showOpenFolderDialog(@Nullable String defaultPath) {
        init();
        return impl.showOpenFolderDialog(defaultPath).exceptionally(FileDialogs::handle);
    }

    private static Optional<String> handle(Throwable e) {
        LOGGER.error("Error opening file dialog", e);
        return Optional.empty();
    }
}
