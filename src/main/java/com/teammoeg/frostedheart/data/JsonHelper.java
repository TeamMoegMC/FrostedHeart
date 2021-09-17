package com.teammoeg.frostedheart.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.util.ResourceLocation;

public class JsonHelper {
	public static final Integer getInt(JsonObject jo,String mem) {
		return getIntOrDefault(jo,mem,null);
	}
	public static final Integer getIntOrDefault(JsonObject jo,String mem,Integer def) {
		JsonElement je=jo.get(mem);
		if(je==null)return def;
		return je.getAsInt();
	}
	public static final Float getFloat(JsonObject jo,String mem) {
		return getFloatOrDefault(jo,mem,null);
	}
	public static final Float getFloatOrDefault(JsonObject jo,String mem,Float def) {
		JsonElement je=jo.get(mem);
		if(je==null)return def;
		return je.getAsFloat();
	}
	public static final Boolean getBoolean(JsonObject jo,String mem) {
		return getBooleanOrDefault(jo,mem,null);
	}
	public static final Boolean getBooleanOrDefault(JsonObject jo,String mem,Boolean def) {
		JsonElement je=jo.get(mem);
		if(je==null)return def;
		return je.getAsBoolean();
	}
	public static final String getString(JsonObject jo,String mem) {
		return getStringOrDefault(jo,mem,null);
	}
	public static final String getStringOrDefault(JsonObject jo,String mem,String def) {
		JsonElement je=jo.get(mem);
		if(je==null)return def;
		return je.getAsString();
	}
	public static final ResourceLocation getResourceLocation(JsonObject jo,String mem) {
		return new ResourceLocation(jo.get(mem).getAsString());
	}
}
