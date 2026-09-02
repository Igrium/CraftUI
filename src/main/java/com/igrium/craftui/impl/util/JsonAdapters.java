package com.igrium.craftui.impl.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.resources.Identifier;

import java.io.IOException;

@UtilityClass
public final class JsonAdapters {
    public static class IdentifierJsonAdapter extends TypeAdapter<Identifier> {

        @Override
        public void write(JsonWriter out, Identifier value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public Identifier read(JsonReader in) throws IOException {
            return Identifier.parse(in.nextString());
        }

    }

    public static class UVPairJsonAdapter extends TypeAdapter<UVPair> {

        @Override
        public void write(JsonWriter out, UVPair value) throws IOException {
            out.beginArray();
            out.value(value.u());
            out.value(value.v());
            out.endArray();
        }

        @Override
        public UVPair read(JsonReader in) throws IOException {
            JsonToken next = in.peek();
            if (next == JsonToken.BEGIN_ARRAY) {
                in.beginArray();
                float x = (float) in.nextDouble();
                float y = (float) in.nextDouble();
                in.endArray();
                return new UVPair(x, y);
            } else {
                float val = (float) in.nextDouble();
                return new UVPair(val, val);
            }
        }

    }
}
