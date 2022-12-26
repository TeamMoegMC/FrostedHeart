package com.teammoeg.frostedheart.research;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.research.research.Research;

import net.minecraft.network.PacketBuffer;

public class SpecialResearch {
    private static CustomSerializerRegistry<Research,BiFunction<String,JsonObject,Research>> registry=new CustomSerializerRegistry<>();
    static {
    	SpecialResearch.register(Research.class,"default",Research::new, Research::new);
    }
 
    public static void register(Class<? extends Research> cls,String type,BiFunction<String,JsonObject,Research> json,Function<PacketBuffer,Research> packet) {
    	registry.register(cls, type, json, packet);
    }
    private SpecialResearch() {
    }
    public static void writeId(Research e,PacketBuffer pb) {
    	registry.writeId(pb, e);
    }
    public static Research deserialize(String id,JsonObject jo) {
    	BiFunction<String, JsonObject, Research> i=registry.getDeserializeOrDefault(jo,Research::new);
    	
        return i.apply(id, jo);
    }
    public static Research deserialize(PacketBuffer data) {
        return registry.read(data);
    }
    public static void writeType(Research e,JsonObject jo) {
    	if(e.getClass()!=Research.class)
    		registry.writeType(jo, e);
    }
}
