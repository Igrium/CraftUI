package com.igrium.craftui.impl.file;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class ImFileDialogPopup {

    @Getter
    private final CompletableFuture<Optional<String>> future = new CompletableFuture<>();

    @Getter @Setter
    private String defaultPath = ".";

    private final ImBoolean isOpen = new ImBoolean(false);
    private final ImString filepath = new ImString();

    private final ImFileDialogWidget widget;

    public ImFileDialogPopup() {
        widget = new ImFileDialogWidget();
        widget.setCallback(opt -> {
            if (opt.isPresent()) {
                future.complete(Optional.of(opt.get().toString()));
            } else {
                future.complete(Optional.empty());
            }
        });
    }

    public void render() {
        if (!isOpen.get()) {
            ImGui.openPopup("File Dialog");
            filepath.set(defaultPath);
            isOpen.set(true);
            widget.setOpen(true);
        }

        if (!widget.isOpen()) {
            isOpen.set(false);
        }

        if (ImGui.beginPopupModal("File Dialog", isOpen, ImGuiWindowFlags.NoSavedSettings)) {
            widget.render();
//            ImGui.text("Native file dialog failed to open. Please enter filepath.");
//            ImGui.inputText("##File", filepath);
//
//            if (ImGui.button("OK")) {
//                ImGui.closeCurrentPopup();
//                future.complete(Optional.of(filepath.get()));
//            }
//            ImGui.sameLine();
//            if (ImGui.button("Cancel")) {
//                ImGui.closeCurrentPopup();
//            }
//
//            ImGui.endPopup();
            ImGui.endPopup();
        }

        if (!isOpen.get() && !future.isDone() && !future.isCancelled()) {
            future.complete(Optional.empty());
        }
    }
}
