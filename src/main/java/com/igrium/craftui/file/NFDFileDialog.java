package com.igrium.craftui.file;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.nfd.NFDFilterItem;
import org.lwjgl.util.nfd.NativeFileDialog;

import com.igrium.craftui.file.FileDialogs.FileFilter;

import net.minecraft.client.MinecraftClient;

class NFDFileDialog implements FileDialogInterface {
    private static final ThreadLocal<Boolean> initialized = ThreadLocal.withInitial(() -> false);

    private static Executor dialogExecutor;

    // The UI can only be opened on the main thread on Mac. However, this will cause
    // the rest of the game to hang, so only do that if we have to.
    static {
        if (MinecraftClient.IS_SYSTEM_MAC) {
            dialogExecutor = MinecraftClient.getInstance();
        } else {
            dialogExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "File Dialog Thread"));
        }
    }

    private static int tryInit() {
        if (!initialized.get()) {
            int result = NativeFileDialog.NFD_Init();
            if (result != NativeFileDialog.NFD_OKAY) {
                throw new RuntimeException("Unknown native error initializing NativeFileDialog.");
            }

            initialized.set(true);
            return result;
        }
        return NativeFileDialog.NFD_OKAY;
    }

    @Override
    // If NFD_Init fails on one thread, it will likely fail on others. Use this to
    // check if it will launch properly.
    public void init() throws Exception {
        tryInit();
    }

    @Override
    public CompletableFuture<Optional<String>> showSaveDialog(@Nullable String defaultPath,
                                                              @Nullable String defaultName, FileFilter... filters) {
        return CompletableFuture.supplyAsync(() -> saveDialogSync(defaultPath, defaultName, filters), dialogExecutor);
    }

    public Optional<String> saveDialogSync(@Nullable String defaultPath,
            @Nullable String defaultName, FileFilter... filters) {
        tryInit();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer out = stack.callocPointer(1);
            NFDFilterItem.Buffer filter = createFilters(filters, stack);

            int result = NativeFileDialog.NFD_SaveDialog(out, filter, defaultPath, defaultName);

            if (result == NativeFileDialog.NFD_OKAY) {
                String returnVal = out.getStringUTF8(0);
                NativeFileDialog.NFD_FreePath(out.get(0));

                return Optional.of(returnVal);
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public CompletableFuture<Optional<String>> showOpenDialog(@Nullable String defaultPath, FileFilter... filters) {
        return CompletableFuture.supplyAsync(() -> openDialogSync(defaultPath, filters), dialogExecutor);
    }

    public Optional<String> openDialogSync(@Nullable String defaultPath, FileFilter... filters) {
        tryInit();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer out = stack.callocPointer(1);
            NFDFilterItem.Buffer filter = createFilters(filters, stack);

            int result = NativeFileDialog.NFD_OpenDialog(out, filter, defaultPath);

            if (result == NativeFileDialog.NFD_OKAY) {
                String returnVal = out.getStringUTF8(0);
                NativeFileDialog.NFD_FreePath(out.get(0));

                return Optional.of(returnVal);
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public CompletableFuture<Optional<String>> showOpenFolderDialog(@Nullable String defaultPath) {
        return CompletableFuture.supplyAsync(() -> pickFolderSync(defaultPath), dialogExecutor);
    }

    public Optional<String> pickFolderSync(@Nullable String defaultPath) {
        tryInit();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer out = stack.callocPointer(1);

            int result = NativeFileDialog.NFD_PickFolder(out, defaultPath);

            if (result == NativeFileDialog.NFD_OKAY) {
                String returnVal = out.getStringUTF8(0);
                NativeFileDialog.NFD_FreePath(out.get(0));

                return Optional.of(returnVal);
            } else {
                return Optional.empty();
            }
        }
    }

    private static NFDFilterItem.Buffer createFilters(FileFilter[] fileFilters, MemoryStack stack) {
        if (fileFilters == null || fileFilters.length == 0)
            return null;

        var buffer = NFDFilterItem.malloc(fileFilters.length, stack);
        for (int i = 0; i < fileFilters.length; i++) {
            NFDFilterItem item = buffer.get(i);
            FileFilter filter = fileFilters[i];

            List<String> extensions = Arrays.stream(filter.extensions()).map(ext -> {
                ext = ext.trim();
                if (ext.startsWith("*.")) {
                    return ext.substring(2);
                } else if (ext.startsWith("*") || ext.startsWith(".")) {
                    return ext.substring(1);
                } else {
                    return ext;
                }
            }).filter(ext -> !ext.isBlank()).toList();

            String spec = String.join(",", extensions);

            item.name(stack.UTF8(fileFilters[i].name()));
            item.spec(stack.UTF8(spec));
        }
        return buffer;
    }
}
