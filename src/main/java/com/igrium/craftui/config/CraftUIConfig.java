package com.igrium.craftui.config;

public final class CraftUIConfig {

    private boolean preferNativeFileDialog = true;

    public boolean preferNativeFileDialog() {
        return preferNativeFileDialog;
    }

    public void setPreferNativeFileDialog(boolean preferNativeFiloeDialog) {
        this.preferNativeFileDialog = preferNativeFiloeDialog;
    }

    public void copyFrom(CraftUIConfig other) {
        this.preferNativeFileDialog = other.preferNativeFileDialog;
    }
}
