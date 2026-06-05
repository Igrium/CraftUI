package com.igrium.craftui.impl.file;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class ImFileDialogWidget {

    @Getter
    private final CompletableFuture<Optional<String>> future = new CompletableFuture<>();

    @Getter @Setter
    private String defaultPath = ".";

    private final ImBoolean isOpen = new ImBoolean(false);
    private final ImString filepath = new ImString();

    public void render() {
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

        if (!isOpen.get() && !future.isDone() && !future.isCancelled()) {
            future.complete(Optional.empty());
        }
    }
}
