package com.igrium.craftui.testmod;

import cn.enaium.fabric.imgui.TextureBindings;
import com.igrium.craftui.api.controls.ViewportControlHelper;
import com.igrium.craftui.api.controls.ViewportControls;
import com.igrium.craftui.api.icon.FontAwesome;
import com.igrium.craftui.api.CraftUI;
import com.igrium.craftui.api.app.DockSpaceApp;
import com.igrium.craftui.api.file.FileDialogs;
import com.igrium.craftui.api.file.FileDialogs.FileFilter;
import com.igrium.craftui.api.icon.NbtIcons;
import com.igrium.craftui.api.nbt.NbtEditor;
import com.igrium.craftui.api.nbt.NbtEditorFlags;
import com.igrium.craftui.api.style.CraftUILayouts;
import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.igrium.craftui.api.gizmo.GizmoHelper;
import imgui.ImGui;
import imgui.extension.imguizmo.ImGuizmo;
import imgui.extension.imguizmo.flag.Mode;
import imgui.extension.imguizmo.flag.Operation;
import imgui.flag.ImGuiFocusedFlags;
import imgui.flag.ImGuiMouseButton;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.renderer.texture.AbstractTexture;
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
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalDouble;
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

    private final GpuSampler sampler;
    private NbtEditor<?> nbtEditor;

    private final ViewportControlHelper viewportControls = new ViewportControlHelper();

    // GIZMO TEST
    private final ImBoolean showGizmo = new ImBoolean(true);
    private final ImInt gizmoOperation = new ImInt(0);
    private final ImInt gizmoMode = new ImInt(0);
    private final ImBoolean gizmoSnap = new ImBoolean(false);
    private final float[] gizmoSnapValues = new float[]{1, 1, 1};

    private static final int[] GIZMO_OPERATIONS = {Operation.TRANSLATE, Operation.ROTATE, Operation.SCALE, Operation.UNIVERSAL};
    private static final int[] GIZMO_MODES = {Mode.LOCAL, Mode.WORLD};

    // Stored in world-space, independent of the camera, so precision doesn't drift as the camera moves.
    private final Vector3d gizmoPos = new Vector3d();
    private final Quaternionf gizmoRot = new Quaternionf();
    private final Vector3f gizmoScale = new Vector3f(1);
    private boolean gizmoInitialized;

    // Camera-relative model matrix, rebuilt every frame.
    private final Matrix4f gizmoMatrix = new Matrix4f();

    private final float[] gizmoViewBuffer = new float[16];
    private final float[] gizmoProjBuffer = new float[16];
    private final float[] gizmoModelBuffer = new float[16];

    public TestApp() {
        setViewportInputMode(ViewportInputMode.DRAG);
        setViewportInputButtons(ImGuiMouseButton.Right);

        CompoundTag editingNbt = new CompoundTag();

        editingNbt.put("Value1", StringTag.valueOf("Hello"));
        editingNbt.put("Value2", IntTag.valueOf(69));


        editingNbt.put("byteValue", ByteTag.valueOf((byte) 2));
        editingNbt.put("shortValue", ShortTag.valueOf((short) 5));
        editingNbt.put("intValue", IntTag.valueOf(69));
        editingNbt.put("longValue", LongTag.valueOf(69420));
        editingNbt.put("floatValue", FloatTag.valueOf(2.54f));
        editingNbt.put("doubleValue", DoubleTag.valueOf(2124.2));

        editingNbt.put("longArray", new LongArrayTag(new long[]{543, 22, 48}));

        CompoundTag compound = new CompoundTag();
        compound.put("NestedStr", StringTag.valueOf("Hello World!"));

        ListTag list = new ListTag();
        list.add(StringTag.valueOf("Hello World!"));
        compound.put("TestList", list);

        editingNbt.put("Compound", compound);

        nbtEditor = NbtEditor.of(editingNbt);

        sampler = RenderSystem.getDevice().createSampler(
                AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                FilterMode.NEAREST, FilterMode.NEAREST,
                1, OptionalDouble.empty()
        );

        this.closeEvent().addListener(this::closed);
    }

    private void closed() {
        sampler.close();
    }

    private final ImBoolean allowEditNbt = new ImBoolean(true);

    public void render(Minecraft client) {
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

            ImGui.text("Here are some test icons! " + FontAwesome.ICON_ALARM_CLOCK + " " + FontAwesome.ICON_WATER_LADDER + " " + FontAwesome.ICON_PAUSE);

            ImGui.text("Here are some test icons: ");
            ImGui.sameLine();

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
                CraftUI.drawGlobalPopup();
                ImGui.endPopup();
            }


            var creeperTex = Identifier.withDefaultNamespace("textures/entity/creeper/creeper.png");
            AbstractTexture abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(creeperTex);

            long texId = TextureBindings.INSTANCE.textureId(abstractTexture.getTextureView(), sampler);
            ImGui.image(texId, abstractTexture.getTexture().getWidth(0) * 8,
                    abstractTexture.getTexture().getHeight(0) * 8);
        }
        ImGui.end();

        if (beginViewport("Viewport", 0)) {
            ImGui.button("This is a button in the viewport!");
            ImGui.text("This is the viewport!");
            boolean mousePressed = mousePressedOverViewport(0);
            ImGui.text("Mouse down: " + mousePressed);

            // Draw before handling viewport controls / clicks so ImGuizmo.isOver() & isUsing() are current
            if (showGizmo.get()) {
                drawGizmo(client);
            }
            boolean gizmoBusy = showGizmo.get() && (ImGuizmo.isOver() || ImGuizmo.isUsing());

            if (doClickExplosion.get()) {
                if (ImGui.isWindowHovered() && ImGui.isMouseClicked(0) && !gizmoBusy) {
                    raycastExplosion();
                }
            }

            if (ImGui.isWindowHovered() && ImGui.isMouseClicked(2) && !gizmoBusy) {
                clickNbt();
            }

            if (ImGui.isWindowHovered() && ImGui.isMouseReleased(1)) {
                ImGui.openPopup("ctxMenu");
            }

            if (ImGui.beginPopup("ctxMenu")) {
                ImGui.menuItem("This is the context menu");
                ImGui.endPopup();
            }

            viewportControls.handleViewportControls();

            if (ImGui.isWindowHovered()) {
                float scrollY = ImGui.getIO().getMouseWheel();
                if (Math.abs(scrollY) > .01f) {
                    // I eyeballed the -12
                    viewportControls.applyDiscreteMove(ViewportControls.ControlMode.ZOOM, 0, scrollY * -12 );
                }
            }
        }
        ImGui.end();

        // NBT EDITOR
        if (ImGui.begin("NBT Editor")) {
            ImGui.checkbox("Allow Editing NBT", allowEditNbt);
            if (ImGui.button("Open NBT Editor in popup")) {
                ImGui.openPopup("nbtEditor");
            }
            if (ImGui.beginPopup("nbtEditor")) {
                if (nbtEditor.render("NBT editor##test", allowEditNbt.get() ? 0 : NbtEditorFlags.READONLY)) {
                    Tag newVal = nbtEditor.getNbt();
                    if (client.player != null) {
                        client.player.sendSystemMessage(Component.literal("New NBT: " + newVal));
                    }
                }
                ImGui.endPopup();
            }
            ImGui.separator();
            if (nbtEditor.render("NBT editor##test", allowEditNbt.get() ? 0 : NbtEditorFlags.READONLY)) {
                Tag newVal = nbtEditor.getNbt();
                if (client.player != null) {
                    client.player.sendSystemMessage(Component.literal("New NBT: " + newVal));
                }
            }
        }
        ImGui.end();

        // GIZMO SETTINGS
        if (ImGui.begin("Gizmo")) {
            ImGui.checkbox("Show Gizmo", showGizmo);

            ImGui.radioButton("Translate", gizmoOperation, 0);
            ImGui.sameLine();
            ImGui.radioButton("Rotate", gizmoOperation, 1);
            ImGui.sameLine();
            ImGui.radioButton("Scale", gizmoOperation, 2);
            ImGui.sameLine();
            ImGui.radioButton("Universal", gizmoOperation, 3);

            ImGui.radioButton("Local", gizmoMode, 0);
            ImGui.sameLine();
            ImGui.radioButton("World", gizmoMode, 1);

            ImGui.checkbox("Snap", gizmoSnap);
            if (gizmoSnap.get()) {
                ImGui.inputFloat3("Snap Values", gizmoSnapValues);
            }

            ImGui.separator();

            Vector3f euler = gizmoRot.getEulerAnglesXYZ(new Vector3f());
            ImGui.text(String.format("Position: %.3f, %.3f, %.3f", gizmoPos.x, gizmoPos.y, gizmoPos.z));
            ImGui.text(String.format("Rotation: %.1f, %.1f, %.1f", Math.toDegrees(euler.x),
                    Math.toDegrees(euler.y), Math.toDegrees(euler.z)));
            ImGui.text(String.format("Scale: %.3f, %.3f, %.3f", gizmoScale.x, gizmoScale.y, gizmoScale.z));

            Vec3 cam = GizmoHelper.getCameraPos();
            ImGui.text(String.format("Camera: %.3f, %.3f, %.3f", cam.x, cam.y, cam.z));
            ImGui.text("Using: " + ImGuizmo.isUsing() + "  Over: " + ImGuizmo.isOver());

            if (ImGui.button("Reset to Player")) {
                resetGizmo(client);
            }
            ImGui.sameLine();
            if (ImGui.button("Summon TNT at Gizmo")) {
                IntegratedServer server = client.getSingleplayerServer();
                if (server != null) {
                    double x = gizmoPos.x, y = gizmoPos.y, z = gizmoPos.z;
                    server.execute(() -> {
                        PrimedTnt entity = new PrimedTnt(server.overworld(), x, y, z, null);
                        server.overworld().addFreshEntity(entity);
                    });
                }
            }
        }
        ImGui.end();

        RenderEventTester.renderWindow();
    }

    private void resetGizmo(Minecraft client) {
        if (client.player != null) {
            Vec3 pos = client.player.position();
            gizmoPos.set(pos.x, pos.y, pos.z);
        }
        gizmoRot.identity();
        gizmoScale.set(1);
        gizmoInitialized = true;
    }

    /**
     * Draw the test gizmo. Must be called inside the viewport window.
     */
    private void drawGizmo(Minecraft client) {
        if (!gizmoInitialized) {
            resetGizmo(client);
        }

        GizmoHelper.setupGizmos(getCustomViewportBounds());

        // Minecraft model matrices are relative to the camera
        Vec3 cam = GizmoHelper.getCameraPos();
        gizmoMatrix.translation((float) (gizmoPos.x - cam.x), (float) (gizmoPos.y - cam.y),
                        (float) (gizmoPos.z - cam.z))
                .rotate(gizmoRot)
                .scale(gizmoScale);

        // Visualize the full transform with a cube
        GizmoHelper.getViewRotationMatrix().get(gizmoViewBuffer);
        GizmoHelper.getProjectionMatrix().get(gizmoProjBuffer);
        gizmoMatrix.get(gizmoModelBuffer);
        ImGuizmo.drawCubes(gizmoViewBuffer, gizmoProjBuffer, gizmoModelBuffer);

        GizmoHelper.manipulateView(GIZMO_OPERATIONS[gizmoOperation.get()], GIZMO_MODES[gizmoMode.get()],
                gizmoMatrix, null, gizmoSnap.get() ? gizmoSnapValues : null);

        if (ImGuizmo.isUsing()) {
            Vector3f translation = gizmoMatrix.getTranslation(new Vector3f());
            gizmoPos.set(translation.x + cam.x, translation.y + cam.y, translation.z + cam.z);
            gizmoMatrix.getScale(gizmoScale);
            // getNormalizedRotation() assumes the basis columns are already unit length, which they aren't
            // once the matrix is scaled (or after float drift). Its error feeds back every frame and blows up.
            // Skip a degenerate (zero-scale) basis, which would otherwise divide by zero.
            if (gizmoScale.x > 1e-6f && gizmoScale.y > 1e-6f && gizmoScale.z > 1e-6f) {
                gizmoMatrix.getUnnormalizedRotation(gizmoRot).normalize();
            }
        }
    }
    /**
     * If any window is focused, unlock the mouse
     */
    private static void unlockIfWindowFocused() {
        if (ImGui.isWindowFocused(ImGuiFocusedFlags.AnyWindow)) {
            CraftUI.forceMouseUnlock();
        }
    }

    @Override
    @Nullable
    public Identifier getLayoutPreset() {
        return switch (layout.get()) {
            case 0 -> CraftUILayouts.DEFAULT;
            case 1 -> LAYOUT1;
            case 2 -> LAYOUT2;
            default -> null;
        };
    }

    private void raycastExplosion() {
        MouseHandler mouse = Minecraft.getInstance().mouseHandler;

        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) return;

        HitResult raycast = CraftUI.raycastViewport((float) mouse.xpos(), (float) mouse.ypos(), 1000, e -> false,
                false);
        LoggerFactory.getLogger(getClass()).info("Position: {} {} {}", raycast.getLocation().x,
                raycast.getLocation().y, raycast.getLocation().z);

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

        HitResult raycast = CraftUI.raycastViewport((float) mouse.xpos(), (float) mouse.ypos(), 1000,
                e -> !(e instanceof Player), false);
        if (raycast instanceof EntityHitResult entHit) {
            Entity ent = entHit.getEntity();

            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, ent.registryAccess());
            ent.saveWithoutId(output);
            CompoundTag nbt = output.buildResult();
            nbtEditor = NbtEditor.of(nbt);
        }
    }

}
