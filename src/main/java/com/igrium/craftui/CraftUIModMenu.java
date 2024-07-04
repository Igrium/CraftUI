package com.igrium.craftui;

import com.igrium.craftui.app.CraftUIOptionsApp;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class CraftUIModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> CraftUIOptionsApp.createScreen().setParent(parent);
    }
}
