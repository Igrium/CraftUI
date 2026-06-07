package com.igrium.craftui.impl.file;

import com.google.common.collect.ImmutableMap;
import com.igrium.craftui.MaterialIcons;
import com.igrium.craftui.file.FileDialogs.FileFilter;
import imgui.ImGui;
import imgui.flag.*;
import imgui.type.ImString;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.util.Language;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;

/**
 * Draws a fully-featured file browser purely within imgui.
 * Requires a font with MaterialUI icons installed.
 */
public final class ImFileDialogWidget {

    /// === INNER TYPES ===

    private record FileEntry(Path path, BasicFileAttributes attrs) {
    }

    private record BookmarkedFile(Path path, char icon) {
        public static BookmarkedFile create(String relPath, char icon) {
            return new BookmarkedFile(SystemUtils.getUserHome().toPath().resolve(relPath), icon);
        }
    }

    /// === CONSTANTS ===

    private static final Logger LOGGER = LoggerFactory.getLogger(ImFileDialogWidget.class);

    private static final Map<String, BookmarkedFile> BOOKMARKS = ImmutableMap.of(
            "Home", new BookmarkedFile(SystemUtils.getUserHome().toPath(), MaterialIcons.ICON_HOME),
            "Documents", BookmarkedFile.create("Documents", MaterialIcons.ICON_DESCRIPTION),
            "Downloads", BookmarkedFile.create("Downloads", MaterialIcons.ICON_DOWNLOAD),
            "Desktop", BookmarkedFile.create("Desktop", MaterialIcons.ICON_DESKTOP_WINDOWS)
    );

    /// === DIALOG CONFIG ===

    /**
     * Consider us saving a file rather than loading a file
     */
    @Getter
    @Setter
    private boolean saveMode;

    /**
     * We're selecting folders rather than files.
     */
    @Getter
    @Setter
    private boolean dirMode;

    /**
     * If the dialog should be open.
     *
     * @apiNote Does not affect rendering code; is only a flag for the caller to check.
     */
    @Getter
    @Setter
    private boolean open = true;

    /**
     * A mutable list of all the file filters this file browser will use.
     */
    @Getter
    private final List<FileFilter> filters = new ArrayList<>();

    /**
     * The file filter in use. <code>null</code> to use the "any" filter.
     */
    @Getter
    @Setter
    private @Nullable FileFilter currentFilter;

    /// === NAVIGATION STATE ===

    /**
     * The current directory being shown in the explorer
     */
    @Getter
    @NonNull
    private Path path = Paths.get("").toAbsolutePath();

    /**
     * The current selected file
     */
    @Getter
    @NonNull
    private String selectedFile = "";

    private final Deque<Path> backStack = new ArrayDeque<>();
    private final Deque<Path> forwardStack = new ArrayDeque<>();

    /**
     * Once a user has selected a file, this is set to the output path.
     * If canceled, isOpen will return false without this being set.
     */
    @Getter
    private @Nullable Path outPath;

    /// === IO STATE ===

    /**
     * The executor that is used for IO-related functions
     */
    @Getter
    @Setter
    @NonNull
    private Executor executor = Util.getIoWorkerExecutor();

    /**
     * The files currently being rendered
     */
    private final Map<String, FileEntry> files = new ConcurrentSkipListMap<>();
    /// === UI STATE ===

    private final ImString directoryString = new ImString(512);
    private boolean wasDirStringActive = false;

    private float prevButtonBarWidth = 0;

    private final ImString selectedFileText = new ImString(128);
    private boolean wasSelectedFileTextActive = false;

    @Getter
    private boolean selectedFileValid;

    /// === CONSTRUCTOR ===

    public ImFileDialogWidget() {
        executor.execute(this::queryFileStores);
        executor.execute(this::queryDirectory);
    }

    /// === FILTER MANAGEMENT ===

    public void setFilters(Collection<? extends FileFilter> filters) {
        this.filters.clear();
        this.filters.addAll(filters);
    }

    public void setFilters(FileFilter... filters) {
        this.filters.clear();
        // The fact that there's no addAll for arrays annoys me
        this.filters.addAll(Arrays.asList(filters));
    }

    /// === NAVIGATION ===

    public void setSelectedFile(@NonNull String selectedFile) {
        this.selectedFile = selectedFile;
        selectedFileValid = validateSelectedFile();
    }

    public void setPath(@NonNull Path path) {
        setPath(path, true);
    }

    public void setPath(@NonNull Path path, boolean updateBack) {
        path = path.toAbsolutePath();
        if (!this.path.equals(path)) {
            setSelectedFile("");

        }
        if (updateBack) {
            forwardStack.clear();
            backStack.push(this.path);
        }

        this.path = path;
        executor.execute(this::queryDirectory);
        validateSelectedFile();
    }

    public void goForward() {
        if (forwardStack.isEmpty()) return;
        backStack.push(getPath());
        setPath(forwardStack.pop(), false);
    }

    public void goBack() {
        if (backStack.isEmpty()) return;
        forwardStack.push(getPath());
        setPath(backStack.pop(), false);
    }

    /// === IO ===

    private void queryFileStores() {
        // TODO: implement
    }

    private void queryDirectory() {
        synchronized (files) {
            files.clear();
            try (var dirStream = Files.newDirectoryStream(getPath())) {
                dirStream.forEach(file -> {
                    try {
                        files.put(file.getFileName().toString(), new FileEntry(file, Files.readAttributes(file, BasicFileAttributes.class)));
                    } catch (IOException e) {
                        LOGGER.error("Error reading file attributes for {}: {}", file, e);
                    }
                });

            } catch (IOException e) {
                LOGGER.error("Error listing directory at {}: {}", getPath(), e);
            }
        }
    }

    /// === DIALOG CONTROL ===

    public void confirm() {
        setOpen(false);
        outPath = getPath().resolve(selectedFile);
    }

    public void cancel() {
        setOpen(false);
    }

    private boolean validateSelectedFile() {
        FileEntry entry = files.get(selectedFile);
        if (isDirMode()) {
            // Can always save and open current directory
            return selectedFile.isEmpty() || (entry != null && entry.attrs().isDirectory());
        } else {
            if (isSaveMode()) {
                return !selectedFile.isBlank() && (entry == null || !entry.attrs().isDirectory());
            } else {
                return entry != null && !entry.attrs().isDirectory();
            }
        }
    }

    /// === RENDER ===

    /**
     * Render the file chooser
     */
    public void render() {
        ImGui.beginGroup();

        float footerHeight = ImGui.getFrameHeightWithSpacing() + ImGui.getStyle().getItemSpacingY();
        boolean wantOpenConfirm = false;

        ImGui.beginTable("fileBrowser", 2, ImGuiTableFlags.BordersInner | ImGuiTableFlags.Resizable);

        ImGui.tableSetupColumn("sidebar", ImGuiTableColumnFlags.WidthFixed, ImGui.getFontSize() * 10, 0);
        ImGui.tableSetupColumn("center", ImGuiTableColumnFlags.WidthStretch, ImGui.getFontSize() * 96, 1);

        ImGui.pushStyleColor(ImGuiCol.Header, ColorHelper.withAlpha(96, ImGui.getColorU32(ImGuiCol.HeaderHovered)));

        /// === NAVIGATION BUTTONS ===
        ImGui.tableNextColumn();

        ImGui.beginDisabled(backStack.isEmpty());
        if (ImGui.button("" + MaterialIcons.ICON_ARROW_BACK)) {
            goBack();
        }
        ImGui.sameLine();
        ImGui.endDisabled();

        ImGui.beginDisabled(forwardStack.isEmpty());
        if (ImGui.button("" + MaterialIcons.ICON_ARROW_FORWARD)) {
            goForward();
        }
        ImGui.sameLine();
        ImGui.endDisabled();

        Path pathParent = getPath().getParent();
        ImGui.beginDisabled(pathParent == null);
        if (ImGui.button("" + MaterialIcons.ICON_ARROW_UPWARD) && pathParent != null) {
            setPath(pathParent);
        }
        ImGui.endDisabled();
        ImGui.sameLine();

        if (ImGui.button("" + MaterialIcons.ICON_REPLAY)) {
            setPath(path, false);
        }

        /// === DIRECTORY BAR ===
        ImGui.tableNextColumn();
        ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
        ImGui.inputText("##directory", directoryString);

        if (ImGui.isItemActive()) {
            wasDirStringActive = true;
        } else {
            if (wasDirStringActive) {
                setPath(Paths.get(directoryString.get()));
                wasDirStringActive = false;
            } else {
                directoryString.set(path.toString());
            }
        }

        /// === PLACES ===

        ImGui.tableNextRow();
        ImGui.tableNextColumn();
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 5f, 0f);
        if (ImGui.beginChild("sidebar", 0, -footerHeight, ImGuiChildFlags.AlwaysUseWindowPadding)) {
            ImGui.separatorText("Places");
            ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, ImGui.getStyle().getItemSpacingX(), 12f);

            BOOKMARKS.forEach((name, file) -> {
                if (ImGui.selectable(file.icon + " " + name, file.path.equals(getPath()))) {
                    setPath(file.path);
                }
            });

            ImGui.popStyleVar();
            ImGui.endChild();
        }
        ImGui.popStyleVar();

        /// === FILE LIST ===
        ImGui.tableNextColumn();

        if (ImGui.beginTable("##files", 1, ImGuiTableFlags.RowBg | ImGuiTableFlags.ScrollY | ImGuiTableFlags.BordersOuter,
                -1, ImGui.getContentRegionAvailY() - footerHeight)) {
            int idx = 0;
            for (FileEntry file : files.values()) {
                String name = file.path.getFileName().toString();
                boolean isDir = file.attrs.isDirectory();
                char icon = isDir ? MaterialIcons.ICON_FOLDER : MaterialIcons.ICON_TEXT_SNIPPET;
                String label = icon + " " + name;

                ImGui.tableNextRow();
                ImGui.tableNextColumn();

                ImGui.beginDisabled(dirMode && !isDir);

                if (ImGui.selectable(label + "###file" + idx++, name.equals(selectedFile))) {
                    setSelectedFile(name);
                }

                if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0)) {
                    if (isDir) {
                        setPath(file.path);
                    } else if (!dirMode) {
                        if (isSaveMode() && files.containsKey(selectedFile)) {
                            wantOpenConfirm = true;
                        } else {
                            confirm();
                        }
                    }
                }

                ImGui.endDisabled();
            }
            ImGui.endTable();
            if (ImGui.isItemClicked()) {
                setSelectedFile("");
            }
        }

        ImGui.popStyleColor();

        ImGui.endTable();

        /// === FOOTER ===

        if (ImGui.beginTable("footer", 2)) {
            ImGui.tableSetupColumn("txtBar", ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableSetupColumn("buttons", ImGuiTableColumnFlags.WidthFixed);

            ImGui.tableNextColumn();

            ImGui.text("File name: ");
            ImGui.sameLine();
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
            ImGui.inputText("##selectedFile", selectedFileText);

            if (ImGui.isItemActive()) {
                wasSelectedFileTextActive = true;
            } else if (wasSelectedFileTextActive) {
                wasSelectedFileTextActive = false;
                setSelectedFile(selectedFileText.get());
            } else {
                selectedFileText.set(selectedFile);
            }

            ImGui.tableSetColumnIndex(1);

            if (ImGui.button(t("gui.cancel"))) {
                cancel();
            }

            ImGui.sameLine();

            ImGui.beginDisabled(!isSelectedFileValid());

            String confirm;
            if (isSaveMode()) {
                confirm = "gui.craftui.fd_save";
            } else {
                confirm = isDirMode() ? "gui.craftui.fd_selectFolder" : "gui.craftui.fd_selectFile";
            }

            if (ImGui.button(t(confirm))) {
                if (isSaveMode() && files.containsKey(selectedFile)) {
                    wantOpenConfirm = true;
                } else {
                    confirm();
                }
            }
            ImGui.endDisabled();
            ImGui.endTable();

        }

        prevButtonBarWidth = ImGui.getItemRectSizeX();

        ImGui.endGroup();

        String overwriteName = t("gui.craftui.fd_overwrite");

        if (wantOpenConfirm) {
            ImGui.openPopup(overwriteName);
        }
        if (ImGui.beginPopupModal(overwriteName, ImGuiWindowFlags.NoSavedSettings
                | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoMove)) {
            ImGui.text(tt("gui.craftui.fd_exists"));

            if (ImGui.button(t("gui.cancel"))) {
                ImGui.closeCurrentPopup();
            }
            ImGui.sameLine();
            if (ImGui.button(t("gui.ok"))) {
                ImGui.closeCurrentPopup();
                confirm();
            }
            ImGui.endPopup();
        }
    }

    /// === UTILITIES ===

    private static String t(String key) {
        return Language.getInstance().get(key) + "###" + key;
    }

    private static String tt(String key) {
        return Language.getInstance().get(key);
    }
}