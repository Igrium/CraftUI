package com.igrium.craftui.file;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.igrium.craftui.app.CraftApp;

import imgui.extension.imguifiledialog.ImGuiFileDialog;
import imgui.extension.imguifiledialog.flag.ImGuiFileDialogFlags;
import imgui.flag.ImGuiWindowFlags;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;

class ImFileDialogApp extends CraftApp {

    @Getter
    private final CompletableFuture<Optional<String>> future = new CompletableFuture<>();
    private boolean isOpen;
    private boolean hasClosed;

    private static final String BROWSE_KEY = "craftui-file-dialog";

    @Getter
    @Setter
    private String defaultPath = ".";

    @Getter
    @Setter
    private String defaultFilename = ".";

    @Getter
    @Setter
    private String filters = ".*";

    @Getter
    @Setter
    private String title = "Select File";

    @Getter
    @Setter
    private boolean warnOnOverride;

    public ImFileDialogApp() {
    }

    @Override
    protected void render(MinecraftClient client) {
        if (!isOpen && !hasClosed) {
            int flags = warnOnOverride ? ImGuiFileDialogFlags.ConfirmOverwrite : ImGuiFileDialogFlags.None;
            ImGuiFileDialog.openModal(BROWSE_KEY, title, filters, defaultPath, defaultFilename, 1, 7, flags);
            isOpen = true;
        }

        if (ImGuiFileDialog.display(BROWSE_KEY, ImGuiWindowFlags.NoSavedSettings, 200, 400, 800, 600)) {
            ImGuiFileDialog.close();
            if (ImGuiFileDialog.isOk()) {
                future.complete(Optional.of(ImGuiFileDialog.getFilePathName()));
            } else {
                future.complete(Optional.empty());
            }
            
        }
    }

}
