package com.energyxxer.trident.extensions;

import com.energyxxer.trident.sets.JsonLiteralSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class EJsonElement {

    public static String getAsStringOrNull(JsonElement thiz) {
        return (thiz != null && thiz.isJsonPrimitive() && thiz.getAsJsonPrimitive().isString()) ?
                thiz.getAsString() :
                null;
    }

    public static String getAsStringOrNumberOrNull(JsonElement thiz) {
        if(thiz instanceof JsonLiteralSet.CustomJSONNumber) {
            return ((JsonLiteralSet.CustomJSONNumber) thiz).getWrapped();
        }
        return (thiz != null && thiz.isJsonPrimitive() && thiz.getAsJsonPrimitive().isString()) ?
                thiz.getAsString() :
                (thiz != null && thiz.isJsonPrimitive() && thiz.getAsJsonPrimitive().isNumber()) ?
                        String.valueOf(thiz.getAsJsonPrimitive().getAsNumber()) :
                null;
    }

    public static Boolean getAsBooleanOrNull(JsonElement thiz) {
        return (thiz != null && thiz.isJsonPrimitive() && thiz.getAsJsonPrimitive().isBoolean()) ?
                thiz.getAsBoolean() :
                null;
    }

    public static Integer getAsIntegerOrNull(JsonElement thiz) {
        return (thiz != null && thiz.isJsonPrimitive() && thiz.getAsJsonPrimitive().isNumber()) ?
                thiz.getAsInt() :
                null;
    }

    public static JsonObject getAsJsonObjectOrNull(JsonElement thiz) {
        return (thiz != null && thiz.isJsonObject()) ?
                thiz.getAsJsonObject() :
                null;
    }

    public static JsonArray getAsJsonArrayOrNull(JsonElement thiz) {
        return (thiz != null && thiz.isJsonArray()) ?
                thiz.getAsJsonArray() :
                null;
    }

}