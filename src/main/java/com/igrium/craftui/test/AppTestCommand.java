package com.igrium.craftui.test;

import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class AppTestCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandRegistryAccess registryAccess) {
        
        dispatcher.register(literal("apptest").then(
            literal("open").executes(c -> {
                ImGuiTestApp.open();
                return 1;
            })
        ).then(
            literal("close").executes(C -> {
                ImGuiTestApp.close();
                return 1;
            })
        ));
    }
    
}
