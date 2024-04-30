package com.igriu.craftui.test;

import com.mojang.brigadier.CommandDispatcher;

import imgui.app.Application;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class AppTestCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandRegistryAccess registryAccess) {
        
        dispatcher.register(literal("apptest").then(
            literal("open").executes(c -> {
                Application.launch(new ImGuiTestApp());
                return 1;
            })
        ));
    }
    
}
