package com.teammoeg.frostedheart.trade;

import java.util.function.Function;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.research.JsonSerializerRegistry;


import net.minecraft.network.PacketBuffer;

public class Conditions {
    private static JsonSerializerRegistry<PolicyCondition> registry=new JsonSerializerRegistry<>();

    static {
    }
    public static void registerType(Class<? extends PolicyCondition> cls,String type,Function<JsonObject, PolicyCondition> json,Function<PacketBuffer, PolicyCondition> packet) {
    	registry.register(cls, type, json, packet);
    }
    private Conditions() {
    }
    public static void writeId(PolicyCondition e,PacketBuffer pb) {
    	registry.writeId(pb, e);
    }
    public static PolicyCondition deserialize(JsonObject jo) {
        return registry.deserialize(jo);
    }
    public static PolicyCondition deserialize(PacketBuffer data) {
        return registry.read(data);
    }
    public static void writeType(PolicyCondition e,JsonObject jo) {
    	registry.writeType(jo, e);
    }
}
