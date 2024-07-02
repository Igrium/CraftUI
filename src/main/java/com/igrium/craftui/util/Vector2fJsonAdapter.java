package com.igrium.craftui.util;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.minecraft.client.util.math.Vector2f;

public class Vector2fJsonAdapter extends TypeAdapter<Vector2f> {

    @Override
    public void write(JsonWriter out, Vector2f value) throws IOException {
        out.beginArray();
        out.value(value.getX());
        out.value(value.getY());
        out.endArray();
    }

    @Override
    public Vector2f read(JsonReader in) throws IOException {
        in.beginArray();
        float x = (float) in.nextDouble();
        float y = (float) in.nextDouble();
        in.endArray();
        return new Vector2f(x, y);
    }
    
}
