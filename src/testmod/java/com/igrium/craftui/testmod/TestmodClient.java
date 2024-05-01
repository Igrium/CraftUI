package com.igrium.craftui.testmod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class TestmodClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("craftui-test");

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(AppTestCommand::register);
    }
    
}
