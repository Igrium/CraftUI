package com.igrium.craftui.screen;

import com.igrium.craftui.app.AppManager;
import com.igrium.craftui.app.CraftApp;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A Minecraft screen that renders a GUI app while it's open.
 */
public class CraftAppScreen<T extends CraftApp> extends Screen {

    private final T app;

    @Getter @Setter
    private boolean closeOnEsc = true;

    @Getter
    private Screen parent;

    public CraftAppScreen(T app) {
        super(Component.empty());
        this.app = app;
        app.closeEvent().addListener(this::onAppClosed);
    }

    public final T getApp() {
        return app;
    }
    
    @Override
    public void added() {
        super.added();
        if (!app.isOpen()) {
            AppManager.openApp(app);
        }
    }

    @Override
    public void removed() {
        AppManager.closeApp(app);
        super.removed();
    }

    private void onAppClosed() {
        // For some reason, close() doesn't check if the screen's actually open
        if (minecraft.gui.screen() == this) {
            this.onClose();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return closeOnEsc;
    }

    public CraftAppScreen<T> setParent(Screen parent) {
        this.parent = parent;
        return this;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(parent);
    }
}
