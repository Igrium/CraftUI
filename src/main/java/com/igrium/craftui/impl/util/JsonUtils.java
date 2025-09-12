package com.igrium.craftui.impl.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonUtils {
    public enum ConflictStrategy {
        PREFER_FIRST, PREFER_SECOND, PREFER_NON_NULL
    }

    public enum ArrayMergeStrategy {
        REPLACE, PREPEND, APPEND
    }

    public static void extendJsonObject(JsonObject dest, JsonObject addend, ConflictStrategy conflictStrategy,
                                        ArrayMergeStrategy arrayMergeStrategy) {
        for (var entry : addend.entrySet()) {
            JsonElement existing = dest.get(entry.getKey());
            if (existing != null) {
                if (entry.getValue() instanceof JsonArray arr && existing instanceof JsonArray dArr) {
                    switch (arrayMergeStrategy) {
                        case REPLACE -> {
                            if (conflictStrategy == ConflictStrategy.PREFER_SECOND) {
                                dest.add(entry.getKey(), arr.deepCopy());
                            }
                        } case PREPEND -> {
                            var arr2 = new JsonArray(dArr.size() + arr.size());
                            arr2.addAll(arr.deepCopy());
                            arr2.addAll(dArr);
                            dest.add(entry.getKey(), arr2);
                        } case APPEND -> {
                            dArr.addAll(arr.deepCopy());
                        }
                    }
                } else if (entry.getValue() instanceof JsonObject obj && existing instanceof JsonObject dObj) {
                    extendJsonObject(dObj, obj, conflictStrategy, arrayMergeStrategy);
                } else {
                    switch (conflictStrategy) {
                        case PREFER_FIRST -> {
                            // Skip this key
                        } case PREFER_SECOND -> {
                            dest.add(entry.getKey(), entry.getValue());
                        } case PREFER_NON_NULL -> {
                            if (existing.isJsonNull()) {
                                dest.add(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }
            } else {
                dest.add(entry.getKey(), entry.getValue());
            }
        }
    }

    public static void extendJsonObject(JsonObject dest, JsonObject addend, ConflictStrategy conflictStrategy) {
        extendJsonObject(dest, addend, conflictStrategy, ArrayMergeStrategy.REPLACE);
    }
}
