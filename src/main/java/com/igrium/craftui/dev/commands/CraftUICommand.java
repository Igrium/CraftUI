package com.igrium.craftui.dev.commands;

import com.igrium.craftui.CraftAppScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class CraftUICommand {

    private static CraftAppScreen<ImGuiDemoApp> demoApp;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandRegistryAccess registryAccess) {
        
        dispatcher.register(literal("craftui").then(
            literal("demo").executes(CraftUICommand::openDemoApp)
        ));
    }

    private static int openDemoApp(CommandContext<FabricClientCommandSource> context) {
        if (demoApp != null && demoApp.getApp().isOpen()) {
            return 0;
        }

        demoApp = new CraftAppScreen<ImGuiDemoApp>(new ImGuiDemoApp());
        MinecraftClient client = MinecraftClient.getInstance();
        // Let the chat screen close before we try to open this
        client.send(() -> {
            MinecraftClient.getInstance().setScreen(demoApp);
        });
        return 1;
    }

}
