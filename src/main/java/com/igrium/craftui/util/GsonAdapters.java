package com.igrium.craftui.util;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;

public class GsonAdapters {
    public static class IdentifierJsonAdapter extends TypeAdapter<Identifier> {

        @Override
        public void write(JsonWriter out, Identifier value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public Identifier read(JsonReader in) throws IOException {
            try {
                return new Identifier(in.nextString());
            } catch (InvalidIdentifierException e) {
                throw new IOException(e);
            }
        }
        
    }
}
