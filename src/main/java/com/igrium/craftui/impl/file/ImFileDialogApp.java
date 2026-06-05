package com.igrium.craftui.impl.file;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.igrium.craftui.app.CraftApp;
//
//import imgui.extension.imguifiledialog.ImGuiFileDialog;
//import imgui.extension.imguifiledialog.flag.ImGuiFileDialogFlags;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;

class ImFileDialogApp extends CraftApp {

    @Getter
    private final CompletableFuture<Optional<String>> future = new CompletableFuture<>();
//    private boolean isOpen;
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

    private final ImBoolean isOpen = new ImBoolean(false);
    private final ImString filepath = new ImString();

    @Override
    protected void render(MinecraftClient client) {
        if (!isOpen.get()) {
            ImGui.openPopup("File Dialog");
            filepath.set(defaultPath);
            isOpen.set(true);
        }

        if (ImGui.beginPopupModal("File Dialog", isOpen, ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text("Native file dialog failed to open. Please enter filepath.");
            ImGui.inputText("##File", filepath);

            if (ImGui.button("OK")) {
                ImGui.closeCurrentPopup();
                future.complete(Optional.of(filepath.get()));
            }
            ImGui.sameLine();
            if (ImGui.button("Cancel")) {
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }

        if (!isOpen.get()) {
            future.complete(Optional.empty());
        }

    }

}
