package com.igrium.craftui.testmod;

import com.igrium.craftui.app.AppManager;
import com.igrium.craftui.app.CraftApp;
import com.igrium.craftui.screen.CraftAppScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import org.jetbrains.annotations.Nullable;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;


public class AppTestCommand {

    private static CraftAppScreen<?> appInstance;
    @Nullable
    private static CraftApp app;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandRegistryAccess registryAccess) {

        dispatcher.register(literal("apptest").then(
                literal("open").executes(context -> {
                    if (app != null && app.isOpen())
                        return 0;

                    app = new TestApp();
                    AppManager.openApp(app);
                    return 1;
                })
        ).then(
                literal("close").executes(context -> {
                    if (app == null)
                        return 0;

                    app.close();
                    app = null;
                    return 1;
                })
        ));
//        dispatcher.register(literal("apptest").then(
//            literal("open").executes(c -> {
//                if (appInstance != null && appInstance.getApp().isOpen())
//                    return 0;
//                appInstance = new CraftAppScreen<>(new TestApp());
//                // appInstance.setCloseOnEsc(false);
//                // Execute async so chat window has a chance to close
//                MinecraftClient.getInstance().send(() -> {
//                    MinecraftClient.getInstance().setScreen(appInstance);
//                });
//                return 1;
//            })
//        ).then(
//            literal("close").executes(C -> {
//                if (appInstance == null)
//                    return 0;
//
//                appInstance.close();
//                appInstance = null;
//                return 1;
//            })
//        ));
    }
    
}
