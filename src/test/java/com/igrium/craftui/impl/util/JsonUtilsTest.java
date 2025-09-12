package com.igrium.craftui.impl.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {

    @Test
    void testPreferFirstConflict() {
        JsonObject dest = new JsonObject();
        dest.addProperty("a", 1);
        dest.addProperty("b", 2);

        JsonObject addend = new JsonObject();
        addend.addProperty("a", 99);
        addend.addProperty("c", 3);

        JsonUtils.extendJsonObject(dest, addend, JsonUtils.ConflictStrategy.PREFER_FIRST);

        assertEquals(1, dest.get("a").getAsInt());  // Should not be overwritten
        assertEquals(2, dest.get("b").getAsInt());
        assertEquals(3, dest.get("c").getAsInt());
    }

    @Test
    void testPreferSecondConflict() {
        JsonObject dest = new JsonObject();
        dest.addProperty("a", 1);

        JsonObject addend = new JsonObject();
        addend.addProperty("a", 99);

        JsonUtils.extendJsonObject(dest, addend, JsonUtils.ConflictStrategy.PREFER_SECOND);

        assertEquals(99, dest.get("a").getAsInt()); // Should be overwritten
    }

    @Test
    void testPreferNonNullConflict() {
        JsonObject dest = new JsonObject();
        dest.add("a", JsonNull.INSTANCE);

        JsonObject addend = new JsonObject();
        addend.addProperty("a", 42);

        JsonUtils.extendJsonObject(dest, addend, JsonUtils.ConflictStrategy.PREFER_NON_NULL);

        assertEquals(42, dest.get("a").getAsInt()); // Should be overwritten only if dest is null
    }

    @Test
    void testArrayReplace() {
        JsonObject dest = new JsonObject();
        JsonArray arr1 = new JsonArray();
        arr1.add(1);
        dest.add("arr", arr1);

        JsonObject addend = new JsonObject();
        JsonArray arr2 = new JsonArray();
        arr2.add(2);
        addend.add("arr", arr2);

        JsonUtils.extendJsonObject(dest, addend, JsonUtils.ConflictStrategy.PREFER_SECOND, JsonUtils.ArrayMergeStrategy.REPLACE);

        assertEquals(1, dest.getAsJsonArray("arr").size());
        assertEquals(2, dest.getAsJsonArray("arr").get(0).getAsInt());
    }

    @Test
    void testArrayAppend() {
        JsonObject dest = new JsonObject();
        JsonArray arr1 = new JsonArray();
        arr1.add(1);
        dest.add("arr", arr1);

        JsonObject addend = new JsonObject();
        JsonArray arr2 = new JsonArray();
        arr2.add(2);
        addend.add("arr", arr2);

        JsonUtils.extendJsonObject(dest, addend, JsonUtils.ConflictStrategy.PREFER_SECOND, JsonUtils.ArrayMergeStrategy.APPEND);

        assertEquals(2, dest.getAsJsonArray("arr").size());
        assertEquals(1, dest.getAsJsonArray("arr").get(0).getAsInt());
        assertEquals(2, dest.getAsJsonArray("arr").get(1).getAsInt());
    }

    @Test
    void testArrayPrepend() {
        JsonObject dest = new JsonObject();
        JsonArray arr1 = new JsonArray();
        arr1.add(1);
        dest.add("arr", arr1);

        JsonObject addend = new JsonObject();
        JsonArray arr2 = new JsonArray();
        arr2.add(2);
        addend.add("arr", arr2);

        JsonUtils.extendJsonObject(dest, addend, JsonUtils.ConflictStrategy.PREFER_SECOND, JsonUtils.ArrayMergeStrategy.PREPEND);

        assertEquals(2, dest.getAsJsonArray("arr").size());
        assertEquals(2, dest.getAsJsonArray("arr").get(0).getAsInt());
        assertEquals(1, dest.getAsJsonArray("arr").get(1).getAsInt());
    }

    @Test
    void testRecursiveObjectMerge() {
        JsonObject dest = new JsonObject();
        JsonObject destNested = new JsonObject();
        destNested.addProperty("a", 1);
        destNested.addProperty("b", 2);
        dest.add("nested", destNested);

        JsonObject addend = new JsonObject();
        JsonObject addendNested = new JsonObject();
        addendNested.addProperty("a", 10); // Should be overwritten
        addendNested.addProperty("c", 3);  // Should be added
        addend.add("nested", addendNested);

        // Should merge nested objects recursively and overwrite 'a', add 'c'
        JsonUtils.extendJsonObject(dest, addend, JsonUtils.ConflictStrategy.PREFER_SECOND);

        JsonObject resultNested = dest.getAsJsonObject("nested");
        assertEquals(10, resultNested.get("a").getAsInt());
        assertEquals(2, resultNested.get("b").getAsInt());
        assertEquals(3, resultNested.get("c").getAsInt());
    }
}