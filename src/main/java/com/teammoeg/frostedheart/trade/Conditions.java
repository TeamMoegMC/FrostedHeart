package com.teammoeg.frostedheart.trade;

import java.util.function.Function;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.research.JsonSerializerRegistry;
import com.teammoeg.frostedheart.trade.conditions.FlagValueCondition;
import com.teammoeg.frostedheart.trade.conditions.GreaterFlagCondition;
import com.teammoeg.frostedheart.trade.conditions.LevelCondition;
import com.teammoeg.frostedheart.trade.conditions.NotCondition;
import com.teammoeg.frostedheart.trade.conditions.TotalTradeCondition;
import com.teammoeg.frostedheart.trade.conditions.WithFlagCondition;

import net.minecraft.network.PacketBuffer;

public class Conditions {
    private static JsonSerializerRegistry<PolicyCondition> registry=new JsonSerializerRegistry<>();

    static {
    	registerType(LevelCondition.class,"level",LevelCondition::new,LevelCondition::new);
    	registerType(FlagValueCondition.class,"value",FlagValueCondition::new,FlagValueCondition::new);
    	registerType(GreaterFlagCondition.class,"greater",GreaterFlagCondition::new,GreaterFlagCondition::new);
    	registerType(NotCondition.class,"not",NotCondition::new,NotCondition::new);
    	registerType(TotalTradeCondition.class,"total",TotalTradeCondition::new,TotalTradeCondition::new);
    	registerType(WithFlagCondition.class,"has",WithFlagCondition::new,WithFlagCondition::new);
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
