package com.teammoeg.frostedheart.data;

import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;

public class JsonDataHolder {
    protected JsonObject data;

    public JsonDataHolder(JsonObject data) {
        this.data = data;
    }

    public ResourceLocation getId() {
        return this.getResourceLocation("id");
    }

    public Integer getInt(String mem) {
        return JsonHelper.getInt(data, mem);
    }

    public Integer getIntOrDefault(String mem, Integer def) {
        return JsonHelper.getIntOrDefault(data, mem, def);
    }

    public Float getFloat(String mem) {
        return JsonHelper.getFloat(data, mem);
    }

    public Float getFloatOrDefault(String mem, Float def) {
        return JsonHelper.getFloatOrDefault(data, mem, def);
    }

    public Boolean getBoolean(String mem) {
        return JsonHelper.getBoolean(data, mem);
    }

    public Boolean getBooleanOrDefault(String mem, Boolean def) {
        return JsonHelper.getBooleanOrDefault(data, mem, def);
    }

    public String getString(String mem) {
        return JsonHelper.getString(data, mem);
    }

    public String getStringOrDefault(String mem, String def) {
        return JsonHelper.getStringOrDefault(data, mem, def);
    }

    public ResourceLocation getResourceLocation(String mem) {
        return JsonHelper.getResourceLocation(data, mem);
    }
}
