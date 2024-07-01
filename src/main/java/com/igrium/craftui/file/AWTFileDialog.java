package com.igrium.craftui.file;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jetbrains.annotations.Nullable;

import com.igrium.craftui.file.FileDialogs.FileFilter;

public class AWTFileDialog implements FileDialogInterface {

    private static Executor dialogExecutor = Executors
            .newSingleThreadExecutor(r -> new Thread(r, "File Dialog Thread"));

    @Override
    public void init() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException("AWT thinks we're headless, so AWT file dialog will not work.");
        }
    }

    @Override
    public CompletableFuture<Optional<String>> saveDialog(@Nullable String defaultPath,
            @Nullable String defaultName, FileFilter... filters) {
        return CompletableFuture.supplyAsync(() -> saveDialogSync(defaultPath, defaultName, filters), dialogExecutor);
    }

    public Optional<String> saveDialogSync(@Nullable String defaultPath,
            @Nullable String defaultName, FileFilter... filters) {
        FileDialog fd = new FileDialog((Frame) null);
        fd.setMode(FileDialog.SAVE);
        fd.setDirectory(defaultPath);
        fd.setFile(defaultName);
        fd.setModal(true);

        fd.setVisible(true);
        
        return Optional.ofNullable(fd.getFile());
    }

    @Override
    public CompletableFuture<Optional<String>> openDialog(@Nullable String defaultPath, FileFilter... filters) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'openDialog'");
    }

    @Override
    public CompletableFuture<Optional<String>> pickFolder(@Nullable String defaultPath) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'pickFolder'");
    }
    
}
