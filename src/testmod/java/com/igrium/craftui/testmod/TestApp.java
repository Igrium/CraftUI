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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class TestApp extends DockSpaceApp {

    private final ImString imText = new ImString();
    private final ImInt inputMode = new ImInt(2);
    private final ImBoolean doClickExplosion = new ImBoolean();

    private static final String[] INPUT_MODE_OPTIONS = new String[]{"None", "Hold", "Focus", "Always"};

    private final ImInt layout = new ImInt(0);
    private static final String[] LAYOUT_OPTIONS = new String[]{"Default", "Layout 1", "Layout 2"};

    private static final Identifier LAYOUT1 = Identifier.of("craftui-test", "layout1");
    private static final Identifier LAYOUT2 = Identifier.of("craftui-test", "layout2");

//    private final NbtCompound editingNbt = new NbtCompound();

    private NbtEditor<?> nbtEditor;

    public TestApp() {
        setViewportInputMode(ViewportInputMode.DRAG);
        setViewportInputButtons(1, 2);

        NbtCompound editingNbt = new NbtCompound();

        editingNbt.put("Value1", NbtString.of("Hello"));
        editingNbt.put("Value2", NbtInt.of(69));


        editingNbt.put("byteValue", NbtByte.of((byte)2));
        editingNbt.put("shortValue", NbtShort.of((short)5));
        editingNbt.put("intValue", NbtInt.of(69));
        editingNbt.put("longValue", NbtLong.of(69420));
        editingNbt.put("floatValue", NbtFloat.of(2.54f));
        editingNbt.put("doubleValue", NbtDouble.of(2124.2));

        editingNbt.put("longArray", new NbtLongArray(new long[] {543, 22, 48}));

        NbtCompound compound = new NbtCompound();
        compound.put("NestedStr", NbtString.of("Hello World!"));

        NbtList list = new NbtList();
        list.add(NbtString.of("Hello World!"));
        compound.put("TestList", list);

        editingNbt.put("Compound", compound);

        nbtEditor = NbtEditor.of(editingNbt);
    }

    @Override
    protected void onOpen() {
        super.onOpen();
    }

    private final ImBoolean allowEditNbt = new ImBoolean(true);

    protected void render(MinecraftClient client) {
        super.render(client);

        if (ImGui.begin("Upper Window")) {
            boolean clicked = ImGui.button("Open file chooser");
            if (clicked) {
                FileDialogs.showOpenDialog(client.runDirectory.getAbsolutePath(),
                                new FileFilter("Jpeg Files", ".jpg", ".jpeg"),
                                new FileFilter("PNG Files", "png"))
                        .thenAcceptAsync(opt -> {
                            if (opt.isPresent()) {
                                client.player.sendMessage(Text.literal("You chose " + opt.get()), false);
                            } else {
                                client.player.sendMessage(Text.literal("You didn't select a file."), false);
                            }
                        }, client);
            }
            ImGui.inputText("Type some text.", imText);

            boolean closeClicked = ImGui.button("Close App");
            if (closeClicked) {
                close();
            }

            if (ImGui.button("Close Chat Window")) {
                MinecraftClient.getInstance().setScreen(null);
            }

            ImGui.checkbox("Do Click Explosion", doClickExplosion);

            ImGui.alignTextToFramePadding();
            ImGui.text("Here is an NBT icon:");
            ImGui.sameLine();
            NbtIcons.drawIcon(NbtElement.LIST_TYPE);

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
                        if (opt.isPresent()) {
                            client.player.sendMessage(Text.literal("You chose " + opt.get()), false);
                        } else {
                            client.player.sendMessage(Text.literal("You didn't select a file."), false);
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
                NbtElement newVal = nbtEditor.getNbt();
                client.player.sendMessage(Text.literal("New NBT: " + newVal), false);
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
        Mouse mouse = MinecraftClient.getInstance().mouse;
//        float mouseX = ImGui.getMousePosX();
//        float mouseY = ImGui.getMousePosY();

        IntegratedServer server = MinecraftClient.getInstance().getServer();
        if (server == null) return;

        HitResult raycast = RaycastUtils.raycastViewport((float)mouse.getX(), (float)mouse.getY(), 1000, e -> false, false);
        LoggerFactory.getLogger(getClass()).info("Position: {} {} {}", raycast.getPos().x, raycast.getPos().y, raycast.getPos().z);

        if (raycast.getType() != HitResult.Type.MISS) {
            Vec3d pos = raycast.getPos();
            server.execute(() -> {
                TntEntity entity = new TntEntity(server.getOverworld(), pos.x, pos.y, pos.z, null);
                server.getOverworld().spawnEntity(entity);
            });
//            world.addParticle(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }

    private void clickNbt() {
        Mouse mouse = MinecraftClient.getInstance().mouse;
        IntegratedServer server = MinecraftClient.getInstance().getServer();
        if (server == null) return;

        HitResult raycast = RaycastUtils.raycastViewport((float)mouse.getX(), (float)mouse.getY(), 1000, e -> !(e instanceof PlayerEntity), false);
        if (raycast instanceof EntityHitResult entHit) {
            Entity ent = entHit.getEntity();
            if (ent == null) return;

            NbtCompound nbt = ent.writeNbt(new NbtCompound());
            nbtEditor = NbtEditor.of(nbt);
        }
    }

}
