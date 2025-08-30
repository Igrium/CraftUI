package com.igrium.craftui.testmod;

import com.igrium.craftui.app.DockSpaceApp;
import com.igrium.craftui.file.FileDialogs;
import com.igrium.craftui.icon.NbtIcons;
import com.igrium.craftui.util.NbtEditor;
import com.igrium.craftui.util.RaycastUtils;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.entity.TntEntity;
import net.minecraft.nbt.*;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.slf4j.LoggerFactory;

public class TestApp extends DockSpaceApp {

    private final ImString imText = new ImString();
    private final ImInt inputMode = new ImInt(2);
    private final ImBoolean doClickExplosion = new ImBoolean();

    private static final String[] INPUT_MODE_OPTIONS = new String[]{"None", "Hold", "Focus", "Always"};

    private final NbtCompound editingNbt = new NbtCompound();


    public TestApp() {
        setViewportInputMode(ViewportInputMode.NONE);
        setViewportInputButtons(1, 2);

        editingNbt.put("Value1", NbtString.of("Hello"));
        editingNbt.put("Value2", NbtInt.of(69));


        editingNbt.put("byteValue", NbtByte.of((byte)2));
        editingNbt.put("shortValue", NbtShort.of((short)5));
        editingNbt.put("intValue", NbtInt.of(69));
        editingNbt.put("longValue", NbtLong.of(69420));
        editingNbt.put("floatValue", NbtFloat.of(2.54f));
        editingNbt.put("doubleValue", NbtDouble.of(2124.2));

        editingNbt.put("longArray", new NbtLongArray(new long[] {}));

        NbtCompound compound = new NbtCompound();
        compound.put("NestedStr", NbtString.of("Hello World!"));
        editingNbt.put("Compound", compound);
    }

    @Override
    protected void onOpen() {
        super.onOpen();
    }

    protected void render(MinecraftClient client) {
        super.render(client);

        if (ImGui.begin("Upper Window")) {
            boolean clicked = ImGui.button("Open file chooser");
            if (clicked) {
                FileDialogs.showOpenDialog(client.runDirectory.getAbsolutePath(),
                                new FileDialogs.FileFilter("Jpeg Files", ".jpg", ".jpeg"),
                                new FileDialogs.FileFilter("PNG Files", "png"))
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
        }
        ImGui.end();

        // NBT EDITOR
        if (ImGui.begin("NBT Editor")) {
            NbtEditor.drawNbtEditor("NBT Editor##test", editingNbt, 0);
        }
        ImGui.end();
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
}
