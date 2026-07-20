package com.igrium.craftui.testmod;

import com.igrium.craftui.MaterialIcons;
import com.igrium.craftui.app.AppManager;
import com.igrium.craftui.app.DockSpaceApp;
import com.igrium.craftui.file.FileDialogs;
import com.igrium.craftui.file.FileDialogs.FileFilter;
import com.igrium.craftui.icon.NbtIcons;
import com.igrium.craftui.nbt.NbtEditor;
import com.igrium.craftui.nbt.NbtEditorFlags;
import com.igrium.craftui.style.CraftUILayouts;
import com.igrium.craftui.util.RaycastUtils;
import imgui.ImGui;
import imgui.flag.ImGuiFocusedFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class TestApp extends DockSpaceApp {

    private final ImString imText = new ImString();
    private final ImInt inputMode = new ImInt(2);
    private final ImBoolean doClickExplosion = new ImBoolean();

    private static final String[] INPUT_MODE_OPTIONS =
            Arrays.stream(ViewportInputMode.values()).map(Enum::name).toArray(String[]::new);

    private final ImInt layout = new ImInt(0);
    private static final String[] LAYOUT_OPTIONS = new String[]{"Default", "Layout 1", "Layout 2"};

    private static final Identifier LAYOUT1 = Identifier.fromNamespaceAndPath("craftui-test", "layout1");
    private static final Identifier LAYOUT2 = Identifier.fromNamespaceAndPath("craftui-test", "layout2");

//    private final NbtCompound editingNbt = new NbtCompound();

    private NbtEditor<?> nbtEditor;

    public TestApp() {
        setViewportInputMode(ViewportInputMode.DRAG);
        setViewportInputButtons(1, 2);

        CompoundTag editingNbt = new CompoundTag();

        editingNbt.put("Value1", StringTag.valueOf("Hello"));
        editingNbt.put("Value2", IntTag.valueOf(69));


        editingNbt.put("byteValue", ByteTag.valueOf((byte)2));
        editingNbt.put("shortValue", ShortTag.valueOf((short)5));
        editingNbt.put("intValue", IntTag.valueOf(69));
        editingNbt.put("longValue", LongTag.valueOf(69420));
        editingNbt.put("floatValue", FloatTag.valueOf(2.54f));
        editingNbt.put("doubleValue", DoubleTag.valueOf(2124.2));

        editingNbt.put("longArray", new LongArrayTag(new long[] {543, 22, 48}));

        CompoundTag compound = new CompoundTag();
        compound.put("NestedStr", StringTag.valueOf("Hello World!"));

        ListTag list = new ListTag();
        list.add(StringTag.valueOf("Hello World!"));
        compound.put("TestList", list);

        editingNbt.put("Compound", compound);

        nbtEditor = NbtEditor.of(editingNbt);
    }

    @Override
    protected void onOpen() {
        super.onOpen();
    }

    private final ImBoolean allowEditNbt = new ImBoolean(true);

    protected void render(Minecraft client) {
        super.render(client);

        if (ImGui.begin("Upper Window")) {
            boolean clicked = ImGui.button("Open file chooser");
            if (clicked) {
                FileDialogs.showOpenDialog(client.gameDirectory.getAbsolutePath(),
                                new FileFilter("Jpeg Files", ".jpg", ".jpeg"),
                                new FileFilter("PNG Files", "png"))
                        .thenAcceptAsync(opt -> {
                            if (client.player != null) {
                                if (opt.isPresent()) {
                                    client.player.sendSystemMessage(Component.literal("You chose " + opt.get()));
                                } else {
                                    client.player.sendSystemMessage(Component.literal("You didn't select a file."));
                                }
                            }
                        }, client);
            }
            ImGui.inputText("Type some text.", imText);

            boolean closeClicked = ImGui.button("Close App");
            if (closeClicked) {
                close();
            }

            if (ImGui.button("Close Chat Window")) {
                //noinspection DataFlowIssue
                Minecraft.getInstance().setScreenAndShow(null);
            }

            ImGui.checkbox("Do Click Explosion", doClickExplosion);

            ImGui.alignTextToFramePadding();
            ImGui.text("Here is an NBT icon:");
            ImGui.sameLine();
            NbtIcons.drawIcon(Tag.TAG_LIST);

            ImGui.combo("Viewport Input Mode", inputMode, INPUT_MODE_OPTIONS);
            setViewportInputMode(ViewportInputMode.values()[inputMode.get()]);

            ImGui.combo("Layout", layout, LAYOUT_OPTIONS);

            if (ImGui.button("Throw Exception")) {
                throw new RuntimeException("Test Exception");
            }

            ImGui.text("Here are some test icons! " + MaterialIcons.ICON_1K + " " + MaterialIcons.ICON_APPLE + " " + MaterialIcons.ICON_PAUSE);

            ImGui.text("Here are some test icons: ");
            ImGui.sameLine();
//            ImGui.pushFont(MaterialIcons.getFont(), 16);
//            ImGui.text("" + MaterialIcons.ICON_1K + MaterialIcons.ICON_3D_ROTATION + MaterialIcons.ICON_ADMIN_PANEL_SETTINGS + MaterialIcons.ICON_RADIO);
//            ImGui.popFont();

            if (ImGui.button("Open a popup")) {
                ImGui.openPopup("popup");
            }

            if (ImGui.beginPopupModal("popup", new ImBoolean(true))) {
                ImGui.text("This a popup lol");
                CompletableFuture<Optional<String>> dialogFuture = null;
                if (ImGui.button("Open File")) {
                    dialogFuture = FileDialogs.showOpenDialog(null, new FileFilter("Jpeg files", ".jpg", ".jpeg"));
                }
                if (ImGui.button("Open Folder")) {
                    dialogFuture = FileDialogs.showOpenFolderDialog(null);
                }
                if (ImGui.button("Save File")) {
                    dialogFuture = FileDialogs.showSaveDialog(null, "file.png");
                }

                if (dialogFuture != null) {
                    dialogFuture.thenAccept(opt -> {
                        if (client.player != null) {
                            if (opt.isPresent()) {
                                client.player.sendSystemMessage(Component.literal("You chose " + opt.get()));
                            } else {
                                client.player.sendSystemMessage(Component.literal("You didn't select a file."));
                            }
                        }
                    });
                }
                AppManager.drawGlobalPopup();
                ImGui.endPopup();
            }
        }
        ImGui.end();

        if (beginViewport("Viewport", 0)) {
            ImGui.button("This is a button in the viewport!");
            ImGui.text("This is the viewport!");
            boolean mousePressed = mousePressedOverViewport(0);
            ImGui.text("Mouse down: " + mousePressed);

            if (doClickExplosion.get()) {
                if (ImGui.isWindowHovered() && ImGui.isMouseClicked(0)) {
                    raycastExplosion();
                }
            }

            if (ImGui.isWindowHovered() && ImGui.isMouseClicked(2)) {
                clickNbt();
            }

            if (ImGui.isWindowHovered() && ImGui.isMouseReleased(1)) {
                ImGui.openPopup("ctxMenu");
            }

            if (ImGui.beginPopup("ctxMenu")) {
                ImGui.menuItem("This is the context menu");
                ImGui.endPopup();
            }
        }
        ImGui.end();

        // NBT EDITOR
        if (ImGui.begin("NBT Editor")) {
            ImGui.checkbox("Allow Editing NBT", allowEditNbt);
            ImGui.separator();
            if (nbtEditor.render("NBT editor##test", allowEditNbt.get() ? 0 : NbtEditorFlags.READONLY)) {
                Tag newVal = nbtEditor.getNbt();
                if (client.player != null) {
                    client.player.sendSystemMessage(Component.literal("New NBT: " + newVal));
                }
            };
//            NbtEditor.drawNbtEditor("NBT Editor##test", editingNbt, 0);
        }
        ImGui.end();
    }

    /**
     * If any window is focused, unlock the mouse
     */
    private static void unlockIfWindowFocused() {
        if (ImGui.isWindowFocused(ImGuiFocusedFlags.AnyWindow)) {
            AppManager.forceMouseUnlock();
        }
    }

    @Override
    protected @Nullable Identifier getLayoutPreset() {
        return switch (layout.get()) {
            case 0 -> CraftUILayouts.DEFAULT;
            case 1 -> LAYOUT1;
            case 2 -> LAYOUT2;
            default -> null;
        };
    }

    private void raycastExplosion() {
        MouseHandler mouse = Minecraft.getInstance().mouseHandler;
//        float mouseX = ImGui.getMousePosX();
//        float mouseY = ImGui.getMousePosY();

        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) return;

        HitResult raycast = RaycastUtils.raycastViewport((float)mouse.xpos(), (float)mouse.ypos(), 1000, e -> false, false);
        LoggerFactory.getLogger(getClass()).info("Position: {} {} {}", raycast.getLocation().x, raycast.getLocation().y, raycast.getLocation().z);

        if (raycast.getType() != HitResult.Type.MISS) {
            Vec3 pos = raycast.getLocation();
            server.execute(() -> {
                PrimedTnt entity = new PrimedTnt(server.overworld(), pos.x, pos.y, pos.z, null);
                server.overworld().addFreshEntity(entity);
            });
//            world.addParticle(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }

    private void clickNbt() {
        MouseHandler mouse = Minecraft.getInstance().mouseHandler;
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) return;

        HitResult raycast = RaycastUtils.raycastViewport((float)mouse.xpos(), (float)mouse.ypos(), 1000, e -> !(e instanceof Player), false);
        if (raycast instanceof EntityHitResult entHit) {
            Entity ent = entHit.getEntity();

            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ent.registryAccess());
            ent.saveWithoutId(output);
            CompoundTag nbt = output.buildResult();
            nbtEditor = NbtEditor.of(nbt);
        }
    }

}
