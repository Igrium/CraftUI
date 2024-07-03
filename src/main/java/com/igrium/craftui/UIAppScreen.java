package com.igrium.craftui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * A Minecraft screen that renders a GUI app while it's open.
 */
public class UIAppScreen<T extends CraftApp> extends Screen {

    private final T app;

    private boolean isAppOpen;

    public UIAppScreen(Text title, T app) {
        super(title);
        this.app = app;
    }

    public final T getApp() {
        return app;
    }
    
    @Override
    public void onDisplayed() {
        super.onDisplayed();
        if (!isAppOpen) {
            AppManager.openApp(app);
            isAppOpen = true;
        }
    }

    @Override
    public void removed() {
        if (isAppOpen) {
            AppManager.closeApp(app);
            isAppOpen = false;
        }
        super.removed();
    }
}
