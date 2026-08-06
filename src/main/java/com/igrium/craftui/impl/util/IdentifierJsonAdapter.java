package com.igrium.craftui.impl.util;

import java.io.IOException;
import net.minecraft.resources.Identifier;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class IdentifierJsonAdapter extends TypeAdapter<Identifier> {

    @Override
    public void write(JsonWriter out, Identifier value) throws IOException {
        out.value(value.toString());
    }

    @Override
    public Identifier read(JsonReader in) throws IOException {
        return Identifier.parse(in.nextString());
    }
    
}
