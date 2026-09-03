package com.igrium.craftui.api.screen;

import com.igrium.craftui.api.CraftUI;
import com.igrium.craftui.api.app.CraftApp;

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
        // The app will get GC'd when we're done, so no need to manually unregister the listener
        app.closeEvent().addListener(this::onAppClosed);
    }

    public final T getApp() {
        return app;
    }
    
    @Override
    public void added() {
        super.added();
        if (!app.isOpen()) {
            CraftUI.openApp(app);
        }
    }

    @Override
    public void removed() {
        CraftUI.closeApp(app);
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
