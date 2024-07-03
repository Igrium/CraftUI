package com.igrium.craftui.testmod;

import com.igrium.craftui.CraftApp;

import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImString;
import net.minecraft.client.MinecraftClient;

public class TestApp2 extends CraftApp {

    private int count;
    private ImString str = new ImString(5);
    private float[] flt = new float[1];

    @Override
    protected void render(MinecraftClient client) {
        ImGui.begin("Cool Window");

        ImGui.text("Hello World!");

        if (ImGui.button("Close Me")) {
            close();
        }

        ImGui.sameLine();
        ImGui.text(String.valueOf(count));

        ImGui.inputText("string", str, ImGuiInputTextFlags.CallbackResize);
        ImGui.text("Result: " + str.get());

        ImGui.sliderFloat("float", flt, 0, 1);
        ImGui.separator();
        ImGui.text("Extra ");

        ImGui.end();
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
    

}
