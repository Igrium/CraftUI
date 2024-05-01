package com.igrium.craftui.testmod;

import com.igrium.craftui.AppManager;
import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;


public class AppTestCommand {

    private static TestApp appInstance;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandRegistryAccess registryAccess) {
        
        dispatcher.register(literal("apptest").then(
            literal("open").executes(c -> {
                if (appInstance != null)
                    return 0;
                appInstance = new TestApp();
                AppManager.openApp(appInstance);
                return 1;
            })
        ).then(
            literal("close").executes(C -> {
                if (appInstance == null)
                    return 0;
                
                AppManager.closeApp(appInstance);
                appInstance = null;
                return 1;
            })
        ));
    }
    
}
