package com.igrium.craftui.file;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import com.igrium.craftui.file.FileDialogs.FileFilter;

/**
 * An implementation of communicating to the OS to open a file dialog.
 */
public interface FileDialogInterface {
    public void init() throws Exception;

    

    public CompletableFuture<Optional<String>> saveDialog(@Nullable String defaultPath,
            @Nullable String defaultName, FileFilter... filters);

    public CompletableFuture<Optional<String>> openDialog(@Nullable String defaultPath, FileFilter... filters);

    public CompletableFuture<Optional<String>> pickFolder(@Nullable String defaultPath);
}
