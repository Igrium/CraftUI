package com.igrium.craftui.testmod;

import com.igrium.craftui.CraftAppScreen;
import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;


public class AppTestCommand {

    private static CraftAppScreen<?> appInstance;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandRegistryAccess registryAccess) {
        
        dispatcher.register(literal("apptest").then(
            literal("open").executes(c -> {
                if (appInstance != null && appInstance.getApp().isOpen())
                    return 0;
                appInstance = new CraftAppScreen<>(new TestApp2());
                appInstance.setCloseOnEsc(false);
                // Execute async so chat window has a chance to close
                MinecraftClient.getInstance().send(() -> {
                    MinecraftClient.getInstance().setScreen(appInstance);
                });
                return 1;
            })
        ).then(
            literal("close").executes(C -> {
                if (appInstance == null)
                    return 0;
                
                appInstance.close();
                appInstance = null;
                return 1;
            })
        ));
    }
    
}
