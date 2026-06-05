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
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Draws a fully-featured file browser purely within imgui.
 * Requires a font with MaterialUI icons installed.
 */
public final class ImFileDialogWidget {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImFileDialogWidget.class);

    private record FileEntry(Path path, BasicFileAttributes attrs) {
    }

    private record BookmarkedFile(Path path, char icon) {
        public static BookmarkedFile create(String relPath, char icon) {
            return new BookmarkedFile(SystemUtils.getUserHome().toPath().resolve(relPath), icon);
        }
    }

    private static final Map<String, BookmarkedFile> BOOKMARKS = ImmutableMap.of(
            "Home", new BookmarkedFile(SystemUtils.getUserHome().toPath(), MaterialIcons.ICON_HOME),
            "Documents", BookmarkedFile.create("Documents", MaterialIcons.ICON_DESCRIPTION),
            "Downloads", BookmarkedFile.create("Downloads", MaterialIcons.ICON_DOWNLOAD),
            "Desktop", BookmarkedFile.create("Desktop", MaterialIcons.ICON_DESKTOP_WINDOWS)
    );

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
    @Setter
    @NonNull
    private String fileName = "";

    /**
     * The executor that is used for IO-related functions
     */
    @Getter
    @Setter
    @NonNull
    private Executor executor = Util.getIoWorkerExecutor();

    /**
     * Consider us saving a file rather than loading a file
     */
    @Getter @Setter
    private boolean saveMode;

    /**
     * We're selecting folders rather than files.
     */
    @Getter @Setter
    private boolean folderMode;

    /**
     * Will get called when a file is selected or the dialog is closed
     */
    @Setter
    private @Nullable Consumer<Optional<Path>> callback;

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

    private final Deque<Path> backStack = new ArrayDeque<>();
    private final Deque<Path> forwardStack = new ArrayDeque<>();

    private final ImString directoryString = new ImString(512);

    private boolean wasDirStringActive = false;

    private final Deque<FileStore> fileStores = new ConcurrentLinkedDeque<>();

    /**
     * The files currently being rendered
     */
    private final Deque<FileEntry> files = new ConcurrentLinkedDeque<>();

    public void setPath(@NonNull Path path) {
        setPath(path, true);
    }

    public void setPath(@NonNull Path path, boolean updateBack) {
        path = path.toAbsolutePath();
        if (this.path.equals(path)) return;

        setFileName("");

        if (updateBack) {
            forwardStack.clear();
            backStack.push(this.path);
        }

        this.path = path;
        executor.execute(this::queryDirectory);
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


    private void queryFileStores() {
        // Only allow one thread in this block
        synchronized (fileStores) {
            fileStores.clear();
            for (var store : FileSystems.getDefault().getFileStores()) {
                fileStores.add(store);
            }
        }
    }

    private void queryDirectory() {
        synchronized (files) {
            files.clear();
            try (var dirStream = Files.newDirectoryStream(getPath())) {
                dirStream.forEach(file -> {
                    try {
                        files.add(new FileEntry(file, Files.readAttributes(file, BasicFileAttributes.class)));
                    } catch (IOException e) {
                        LOGGER.error("Error reading file attributes for {}", file, e);
                    }
                });

            } catch (IOException e) {
                LOGGER.error("Error listing directory at {}", getPath(), e);
            }
        }
    }

    public ImFileDialogWidget() {
        executor.execute(this::queryFileStores);
        executor.execute(this::queryDirectory);
    }

    /**
     * Render the file chooser
     */
    public void render() {
        ImGui.beginGroup();

        float footerHeight = ImGui.getFrameHeightWithSpacing();

        ImGui.beginTable("fileBrowser", 2, ImGuiTableFlags.BordersInner | ImGuiTableFlags.Resizable);

        ImGui.tableSetupColumn("sidebar", ImGuiTableColumnFlags.WidthFixed, ImGui.getFontSize() * 10, 0);
        ImGui.tableSetupColumn("center", ImGuiTableColumnFlags.WidthStretch);

        ImGui.pushStyleColor(ImGuiCol.Header, ColorHelper.withAlpha(96, ImGui.getColorU32(ImGuiCol.HeaderHovered)));

//        ImGUi.tableSetup

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

        /// === Directory ===
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
        -1, ImGui.getContentRegionAvailY() - ImGui.getTextLineHeightWithSpacing())) {
            int idx = 0;
            for (FileEntry file : files) {
                String name = file.path.getFileName().toString();
                boolean isDir = file.attrs.isDirectory();
                char icon = isDir ? MaterialIcons.ICON_FOLDER : MaterialIcons.ICON_TEXT_SNIPPET;
                String label = icon + " " + name;

                ImGui.tableNextRow();
                ImGui.tableNextColumn();
                if (ImGui.selectable(label + "###file" + idx++, name.equals(fileName))) {
                    setFileName(name);
                }

                if (isDir && ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0)) {
                    setPath(file.path);
                }
            }
            ImGui.endTable();
        }

        ImGui.popStyleColor();

        ImGui.endTable();
        ImGui.endGroup();
    }

    class MyFileVisitor extends SimpleFileVisitor<Path> {
        @Override
        public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
            return super.visitFile(file, attrs);
        }
    }
}
