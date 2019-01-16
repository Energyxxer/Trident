package Trident.extensions.com.google.gson.JsonElement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class EJsonElement {

    public static String getAsStringOrNull(JsonElement thiz) {
        return (thiz != null && thiz.isJsonPrimitive() && thiz.getAsJsonPrimitive().isString()) ?
                thiz.getAsString() :
                null;
    }

    public static Boolean getAsBooleanOrNull(JsonElement thiz) {
        return (thiz != null && thiz.isJsonPrimitive() && thiz.getAsJsonPrimitive().isBoolean()) ?
                thiz.getAsBoolean() :
                null;
    }

    public static JsonObject getAsJsonObjectOrNull(JsonElement thiz) {
        return (thiz != null && thiz.isJsonObject()) ?
                thiz.getAsJsonObject() :
                null;
    }

}