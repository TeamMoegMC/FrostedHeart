package com.teammoeg.frostedheart.trade;

import java.util.function.Function;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.research.JsonSerializerRegistry;
import com.teammoeg.frostedheart.trade.actions.AddFlagValueAction;
import com.teammoeg.frostedheart.trade.actions.SetFlagAction;
import com.teammoeg.frostedheart.trade.actions.SetFlagValueAction;

import net.minecraft.network.PacketBuffer;

public class Actions {
    private static JsonSerializerRegistry<PolicyAction> registry=new JsonSerializerRegistry<>();

    static {
    	registerType(AddFlagValueAction.class,"add",AddFlagValueAction::new,AddFlagValueAction::new);
    	registerType(SetFlagValueAction.class,"set",SetFlagValueAction::new,SetFlagValueAction::new);
    	registerType(SetFlagAction.class,"flag",SetFlagAction::new,SetFlagAction::new);
    }
    public static void registerType(Class<? extends PolicyAction> cls,String type,Function<JsonObject, PolicyAction> json,Function<PacketBuffer, PolicyAction> packet) {
    	registry.register(cls, type, json, packet);
    }
    private Actions() {
    }
    public static void writeId(PolicyAction e,PacketBuffer pb) {
    	registry.writeId(pb, e);
    }
    public static PolicyAction deserialize(JsonObject jo) {
        return registry.deserialize(jo);
    }
    public static PolicyAction deserialize(PacketBuffer data) {
        return registry.read(data);
    }
    public static void writeType(PolicyAction e,JsonObject jo) {
    	registry.writeType(jo, e);
    }
}
