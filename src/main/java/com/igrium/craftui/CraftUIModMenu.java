package com.igrium.craftui;

import com.igrium.craftui.app.CraftUIConfigApp;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class CraftUIModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> CraftUIConfigApp.createScreen().setParent(parent);
    }
}
