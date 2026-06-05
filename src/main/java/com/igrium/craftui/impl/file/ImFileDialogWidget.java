package com.igrium.craftui.impl.file;

import com.igrium.craftui.MaterialIcons;
import com.igrium.craftui.file.FileDialogs;
import com.igrium.craftui.file.FileDialogs.FileFilter;
import imgui.ImGui;
import imgui.flag.*;
import imgui.type.ImString;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Draws a fully-featured file browser purely within imgui.
 * Requires a font with MaterialUI icons installed.
 */
public class ImFileDialogWidget {
    /**
     * The current directory being shown in the explorer
     */
    @Getter @Setter @NonNull
    private Path directory = Paths.get("/");

    /**
     * The current selected file
     */
    @Getter @Setter @NonNull
    private String fileName = "";

    /**
     * The executor that is used for IO-related functions
     */
    @Getter @Setter @NonNull
    private Executor executor = Util.getIoWorkerExecutor();

    /**
     * Will get called when a file is selected or the dialog is closed
     */
    @Setter
    private @Nullable Consumer<Optional<Path>> callback;

    /**
     * If the dialog should be open.
     * @apiNote Does not affect rendering code; is only a flag for the caller to check.
     */
    @Getter @Setter
    private boolean open = true;

    /**
     * A mutable list of all the file filters this file browser will use.
     */
    @Getter
    private final List<FileFilter> filters = new ArrayList<>();

    public void setFilters(Collection<? extends FileFilter> filters) {
        this.filters.clear();
        this.filters.addAll(filters);
    }

    public void setFilters(FileFilter... filters) {
        this.filters.clear();
        // The fact that there's no addAll for arrays annoys me
        this.filters.addAll(Arrays.asList(filters));
    }

    /**
     * The file filter in use. <code>null</code> to use the "any" filter.
     */
    @Getter @Setter
    private @Nullable FileFilter currentFilter;

    private final ImString directoryString = new ImString(512);

    private boolean wasDirStringActive = false;

    /**
     * Render the file chooser
     */
    public void render() {
//        ImGui.beginGroup();

        float footerHeight = ImGui.getFrameHeightWithSpacing();

        ImGui.beginTable("fileBrowser", 2, ImGuiTableFlags.BordersInner);

        ImGui.tableSetupColumn("sidebar", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("center", ImGuiTableColumnFlags.WidthStretch);

//        ImGUi.tableSetup

        /// === NAVIGATION BUTTONS ===
        ImGui.tableNextColumn();

        ImGui.button("" + MaterialIcons.ICON_ARROW_BACK);
        ImGui.sameLine();
        ImGui.button("" + MaterialIcons.ICON_ARROW_FORWARD);
        ImGui.sameLine();
        ImGui.button("" + MaterialIcons.ICON_ARROW_UPWARD);

        /// === Directory ===
        ImGui.tableNextColumn();
        ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
        ImGui.inputText("##directory", directoryString);

        if (ImGui.isItemActive()) {
            wasDirStringActive = true;
        } else {
            if (wasDirStringActive) {
                setDirectory(Paths.get(directoryString.get()));
                wasDirStringActive = false;
            } else {
                directoryString.set(directory.toString());
            }
        }

        /// === PLACES ===

        ImGui.tableNextRow();
        ImGui.tableNextColumn();
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 5f, 0f);
        if (ImGui.beginChild("sidebar", 0, -footerHeight, ImGuiChildFlags.AlwaysUseWindowPadding)) {
            ImGui.separatorText("Places");
            ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, ImGui.getStyle().getItemSpacingX(), 12f);

            ImGui.selectable(MaterialIcons.ICON_HOME + " Home");
            ImGui.selectable(MaterialIcons.ICON_DOWNLOAD + " Downloads");
            ImGui.selectable(MaterialIcons.ICON_DESKTOP_WINDOWS + " Desktop");
            ImGui.selectable(MaterialIcons.ICON_FOLDER + " Documents");
            ImGui.selectable(MaterialIcons.ICON_MUSIC_NOTE + " Music");
            ImGui.selectable(MaterialIcons.ICON_PHOTO + " Pictures");
            ImGui.selectable(MaterialIcons.ICON_VIDEO_LABEL + " Videos");

            ImGui.popStyleVar();
        }
        ImGui.endChild();
        ImGui.popStyleVar();

        ImGui.endTable();

    }
}
