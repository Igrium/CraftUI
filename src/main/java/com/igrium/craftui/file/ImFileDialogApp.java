package com.igrium.craftui.file;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.igrium.craftui.CraftApp;

import imgui.extension.imguifiledialog.ImGuiFileDialog;
import imgui.extension.imguifiledialog.flag.ImGuiFileDialogFlags;
import imgui.flag.ImGuiWindowFlags;
import net.minecraft.client.MinecraftClient;

public class ImFileDialogApp extends CraftApp {

    private final CompletableFuture<Optional<String>> future = new CompletableFuture<>();
    private boolean isOpen;
    private boolean hasClosed;

    private final String browseKey = new String("craftui-file-dialog");

    private String defaultPath = ".";

    public String getDefaultPath() {
        return defaultPath;
    }

    public void setDefaultPath(String defaultPath) {
        this.defaultPath = defaultPath;
    }

    private String defaultFilename = ".";

    public String getDefaultFilename() {
        return defaultFilename;
    }

    public void setDefaultFilename(String defaultFilename) {
        this.defaultFilename = defaultFilename;
    }

    private String filters = ".*";

    public String getFilters() {
        return filters;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }

    private String title = "Select File";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    private boolean warnOnOverride;

    public boolean isWarnOnOverride() {
        return warnOnOverride;
    }

    public void setWarnOnOverride(boolean warnOnOverride) {
        this.warnOnOverride = warnOnOverride;
    }

    @Override
    protected void render(MinecraftClient client) {
        if (!isOpen && !hasClosed) {
            int flags = warnOnOverride ? ImGuiFileDialogFlags.ConfirmOverwrite : ImGuiFileDialogFlags.None;

            ImGuiFileDialog.openModal(browseKey, title, filters, defaultPath, defaultFilename, 1, 7, flags);
            isOpen = true;
        }

        if (ImGuiFileDialog.display(browseKey, ImGuiWindowFlags.None, 200, 400, 800, 600)) {
            ImGuiFileDialog.close();
            if (ImGuiFileDialog.isOk()) {
                future.complete(Optional.of(ImGuiFileDialog.getFilePathName()));
            } else {
                future.complete(Optional.empty());
            }
            
        }
    }

    public CompletableFuture<Optional<String>> getFuture() {
        return future;
    }
    
}
