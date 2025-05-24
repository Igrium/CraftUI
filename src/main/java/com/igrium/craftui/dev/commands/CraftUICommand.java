package com.igrium.craftui.dev.commands;

import com.igrium.craftui.screen.CraftAppScreen;
import com.igrium.craftui.app.AppManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class CraftUICommand {

    private static CraftAppScreen<ImGuiDemoApp> demoApp;
    private static ImGuiDemoApp screenlessDemoApp;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandRegistryAccess registryAccess) {

        dispatcher.register(literal("craftui").then(
                literal("demo").executes(CraftUICommand::openDemoScreen).then(
                        literal("open").executes(CraftUICommand::openDemo)
                ).then(
                        literal("close").executes(CraftUICommand::closeDemo)
                )
        ));
    }

    private static int openDemoScreen(CommandContext<FabricClientCommandSource> context) {
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

    private static int openDemo(CommandContext<FabricClientCommandSource> context) {
        if (screenlessDemoApp != null && screenlessDemoApp.isOpen())
            return 0;

        screenlessDemoApp = new ImGuiDemoApp();
        AppManager.openApp(screenlessDemoApp);
        return 1;
    }

    private static int closeDemo(CommandContext<FabricClientCommandSource> context) {
        if (screenlessDemoApp == null)
            return 0;

        screenlessDemoApp.close();
        screenlessDemoApp = null;
        return 1;
    }
}
