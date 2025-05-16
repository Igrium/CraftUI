package com.igrium.craftui;

import com.igrium.craftui.app.AppManager;
import com.igrium.craftui.app.CraftApp;

import imgui.ImGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * A Minecraft screen that renders a GUI app while it's open.
 */
public class CraftAppScreen<T extends CraftApp> extends Screen {

    private final T app;

    private boolean closeOnEsc = true;

    private Screen parent;

    public CraftAppScreen(T app) {
        super(Text.empty());
        this.app = app;
        app.closeEvent().addListener(this::onAppClosed);
    }

    public final T getApp() {
        return app;
    }
    
    @Override
    public void onDisplayed() {
        super.onDisplayed();
        if (!app.isOpen()) {
            AppManager.openApp(app);
        }
    }

    @Override
    public void removed() {
        if (app.isOpen()) {
            AppManager.closeApp(app);
        }
        super.removed();
    }

    private void onAppClosed() {
        // For some reason, close() doesn't check if the screen's actually open
        if (client.currentScreen == this) {
            this.close();
        }
    }

    public void setCloseOnEsc(boolean closeOnEscape) {
        this.closeOnEsc = closeOnEscape;
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return closeOnEsc;
    }

    public Screen getParent() {
        return parent;
    }

    public CraftAppScreen<T> setParent(Screen parent) {
        this.parent = parent;
        return this;
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
