package com.igrium.craftui.impl.file;

import com.google.common.collect.ImmutableMap;
import com.igrium.craftui.icon.FontAwesome;
import com.igrium.craftui.file.FileDialogs.FileFilter;
import imgui.ImGui;
import imgui.flag.*;
import imgui.type.ImString;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.util.Util;
import net.minecraft.locale.Language;
import net.minecraft.util.ARGB;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * Draws a fully-featured file browser purely within imgui.
 * Requires a font with Font Awesome icons installed.
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
            "Home", new BookmarkedFile(SystemUtils.getUserHome().toPath(), FontAwesome.ICON_HOUSE),
            "Documents", BookmarkedFile.create("Documents", FontAwesome.ICON_FILE_LINES),
            "Downloads", BookmarkedFile.create("Downloads", FontAwesome.ICON_DOWNLOAD),
            "Desktop", BookmarkedFile.create("Desktop", FontAwesome.ICON_DESKTOP)
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
    private Executor executor = Util.ioPool();

    /**
     * The files currently being rendered
     */
    private final Map<String, FileEntry> files = new ConcurrentSkipListMap<>();

    /// === UI STATE ===

    private final ImString directoryString = new ImString(512);
    private boolean wasDirStringActive = false;

    private final ImString selectedFileText = new ImString(128);
    private boolean wasSelectedFileTextActive = false;

    @Getter
    private boolean selectedFileValid;

    private @Nullable String contextItem;

    private final ImString renameText = new ImString(128);
    private boolean renameTextValid = false;

    /**
     * The name of the file being renamed.
     * <code>null</code> if we're making a new folder
     */
    private @Nullable String renameSrc;

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

    private void newFolder() {
        String name = renameText.get();
        if (name.isBlank()) return;

        try {
            Files.createDirectory(getPath().resolve(name));
        } catch (IOException e) {
            LOGGER.error("Could not create directory {}", name, e);
        }
        renameText.clear();
        renameTextValid = false;
        setPath(path, false);
    }

    private void rename() {
        String name = renameText.get();
        String oldName = renameSrc;
        if (oldName == null || oldName.isBlank()) return;

        Path src = getPath().resolve(oldName);
        Path dest = getPath().resolve(name);

        try {
            Files.move(src, dest);
        } catch (IOException e) {
            LOGGER.error("Could not rename {}", name, e);
        }
        setPath(path, false);
    }

    private void queryFileStores() {
        // TODO: implement
    }

    private void delete() {
        if (contextItem == null || contextItem.isBlank()) return;
        Path path = getPath().resolve(contextItem);
        if (Files.isDirectory(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(f -> {
                            try {
                                Files.delete(f);
                            } catch (IOException e) {
                                LOGGER.error("Could not delete {}", f, e);
                            }
                        });
            } catch (IOException e) {
                LOGGER.error("Could not delete directory {}", path, e);
            }
        } else {
            try {
                Files.delete(path);
            } catch (IOException e) {
                LOGGER.error("Could not delete {}", path, e);
            }
        }
        setPath(getPath(), false);
        contextItem = null;
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

    private void validateRenameText() {
        String name = renameText.get();
        renameTextValid = !name.isBlank() && !Files.exists(getPath().resolve(name));
    }

    /**
     * Draw a confirmation dialog
     * @param name Dialog name (use to open/close)
     * @param text Text to use
     * @return 0 = no response, 1 = confirm, 2 = cancel
     */
    private int drawConfirmDialog(String name, String text) {
        int result = 0;
        if (ImGui.beginPopupModal(name, ImGuiWindowFlags.NoSavedSettings
                | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoMove)) {
            ImGui.text(text);

            if (ImGui.button(t("gui.cancel"))) {
                ImGui.closeCurrentPopup();
                result = 2;
            }
            ImGui.sameLine();
            if (ImGui.isWindowAppearing()) {
                ImGui.setKeyboardFocusHere();
            }
            if (ImGui.button(t("gui.ok"))) {
                ImGui.closeCurrentPopup();
                result = 1;
            }

            if (ImGui.shortcut(ImGuiKey.Enter)) {
                ImGui.closeCurrentPopup();
                result = 1;
            }
            if (ImGui.shortcut(ImGuiKey.Escape)) {
                ImGui.closeCurrentPopup();
                result = 2;
            }

            ImGui.endPopup();
        }
        return result;
    }

    /// === RENDER ===

    /**
     * Render the file chooser
     */
    public void render() {
        ImGui.beginGroup();

        float footerHeight = ImGui.getFrameHeightWithSpacing() + ImGui.getStyle().getItemSpacingY();
        boolean wantConfirmOverride = false;

        ImGui.beginTable("fileBrowser", 2, ImGuiTableFlags.BordersInner | ImGuiTableFlags.Resizable);

        ImGui.tableSetupColumn("sidebar", ImGuiTableColumnFlags.WidthFixed, ImGui.getFontSize() * 10, 0);
        ImGui.tableSetupColumn("center", ImGuiTableColumnFlags.WidthStretch, ImGui.getFontSize() * 96, 1);

        ImGui.pushStyleColor(ImGuiCol.Header, ARGB.color(96, ImGui.getColorU32(ImGuiCol.HeaderHovered)));

        /// === NAVIGATION BUTTONS ===
        ImGui.tableNextColumn();

        ImGui.beginDisabled(backStack.isEmpty());
        if (ImGui.button("" + FontAwesome.ICON_ARROW_LEFT)) {
            goBack();
        }
        ImGui.sameLine();
        ImGui.endDisabled();

        ImGui.beginDisabled(forwardStack.isEmpty());
        if (ImGui.button("" + FontAwesome.ICON_ARROW_RIGHT)) {
            goForward();
        }
        ImGui.sameLine();
        ImGui.endDisabled();

        Path pathParent = getPath().getParent();
        ImGui.beginDisabled(pathParent == null);
        if (ImGui.button("" + FontAwesome.ICON_ARROW_UP) && pathParent != null) {
            setPath(pathParent);
        }
        ImGui.endDisabled();
        ImGui.sameLine();

        if (ImGui.button("" + FontAwesome.ICON_ROTATE_RIGHT)) {
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
            ImGui.separatorText(tt("gui.craftui.fd_places"));
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

        boolean wantOpenCtxMenu = false;

        if (ImGui.beginTable("##files", 1, ImGuiTableFlags.RowBg | ImGuiTableFlags.ScrollY | ImGuiTableFlags.BordersOuter,
                -1, ImGui.getContentRegionAvailY() - footerHeight)) {
            int idx = 0;
            for (FileEntry file : files.values()) {
                String name = file.path.getFileName().toString();
                boolean isDir = file.attrs.isDirectory();
                char icon = isDir ? FontAwesome.ICON_FOLDER : FontAwesome.ICON_FILE_LINES;
                String label = icon + " " + name;

                ImGui.tableNextRow();
                ImGui.tableNextColumn();

                ImGui.beginDisabled(dirMode && !isDir);

                if (ImGui.selectable(label + "###file" + idx++, name.equals(selectedFile))) {
                    setSelectedFile(name);
                }

                if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left)) {
                    if (isDir) {
                        setPath(file.path);
                    } else if (!dirMode) {
                        if (isSaveMode() && files.containsKey(selectedFile)) {
                            wantConfirmOverride = true;
                        } else {
                            confirm();
                        }
                    }
                }

                if (ImGui.isItemClicked(ImGuiMouseButton.Right)) {
                    contextItem = name;
                    wantOpenCtxMenu = true;
                }

                ImGui.endDisabled();
            }
            ImGui.endTable();
            if (ImGui.isItemClicked()) {
                setSelectedFile("");
            }

            if (ImGui.isItemClicked(ImGuiMouseButton.Right) && !wantOpenCtxMenu) {
                contextItem = null;
                wantOpenCtxMenu = true;
            }
        }

        boolean wantConfirmDelete = false;
        if (ImGui.shortcut(ImGuiKey.Delete) || ImGui.shortcut(ImGuiKey.Backspace)) {
            contextItem = selectedFile;
            wantConfirmDelete = true;
        }

        /// === DIALOGS ===

        if (wantOpenCtxMenu) {
            ImGui.openPopup("ctxMenu");
        }

        boolean wantEditName = false;

        if (ImGui.beginPopup("ctxMenu")) {
            ImGui.beginDisabled(contextItem == null);

            if (ImGui.selectable(t("gui.craftui.fd_rename"))) {
                ImGui.closeCurrentPopup();
                renameSrc = contextItem;
                renameText.set(contextItem);
                validateRenameText();
                wantEditName = true;
            }
            if (ImGui.selectable(t("gui.craftui.fd_delete"))) {
                ImGui.closeCurrentPopup();
                wantConfirmDelete = true;
            }

            ImGui.endDisabled();

            if (ImGui.selectable(t("gui.craftui.fd_newFolder"))) {
                contextItem = null;
                renameSrc = null;
                renameText.set("");
                renameTextValid = false;
                ImGui.closeCurrentPopup();
                wantEditName = true;
            }

            ImGui.endPopup();
        }

        if (wantEditName) {
            ImGui.openPopup("editName");
        }

        if (ImGui.beginPopup("editName")) {
            int bg = renameTextValid ? ImGui.getColorU32(ImGuiCol.FrameBg) : 0xFF000066; // red
            ImGui.pushStyleColor(ImGuiCol.FrameBg, bg);
            if (ImGui.isWindowAppearing()) {
                ImGui.setKeyboardFocusHere();
            }
            if (ImGui.inputText(t("gui.craftui.fd_name"), renameText)) {
                validateRenameText();
            }
            ImGui.popStyleColor();
            if (ImGui.isItemDeactivated()) {
                ImGui.closeCurrentPopup();
                validateRenameText();
                if (renameTextValid) {
                    if (renameSrc != null && !renameSrc.isBlank()) {
                        rename();
                    } else {
                        newFolder();
                    }
                }
                renameText.clear();
            }
            ImGui.endPopup();
        }


        ImGui.popStyleColor();

        ImGui.endTable();

        /// === FOOTER ===

        if (ImGui.beginTable("footer", 2)) {
            ImGui.tableSetupColumn("txtBar", ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableSetupColumn("buttons", ImGuiTableColumnFlags.WidthFixed);

            ImGui.tableNextColumn();

            ImGui.text(tt("gui.craftui.fd_fileName"));
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
                    wantConfirmOverride = true;
                } else {
                    confirm();
                }
            }
            ImGui.endDisabled();
            ImGui.endTable();

        }

        ImGui.endGroup();

        String overwriteName = t("gui.craftui.fd_overwrite");

        if (wantConfirmOverride) {
            ImGui.openPopup(overwriteName);
        }
        if (drawConfirmDialog(overwriteName, tt("gui.craftui.fd_exists")) == 1) {
            confirm();
        }

        String deleteName = tt("gui.craftui.fd_deleteConfirm").formatted(contextItem) + "###" + "deleteConfirm";

        if (wantConfirmDelete) {
            ImGui.openPopup(deleteName);
        }
        if (drawConfirmDialog(deleteName, tt("gui.craftui.fd_deleteWarning")) == 1) {
            delete();
        }

    }

    /// === UTILITIES ===

    private static String t(String key) {
        return Language.getInstance().getOrDefault(key) + "###" + key;
    }

    private static String tt(String key) {
        return Language.getInstance().getOrDefault(key);
    }

}