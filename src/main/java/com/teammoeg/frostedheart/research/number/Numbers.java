package com.teammoeg.frostedheart.research.number;

import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.teammoeg.frostedheart.research.JsonSerializerRegistry;
import net.minecraft.network.PacketBuffer;

public class Numbers {
	static JsonSerializerRegistry<IResearchNumber> registry=new JsonSerializerRegistry<>();
    static {
    	registry.register(ConstResearchNumber.class,"value", ConstResearchNumber::valueOf, ConstResearchNumber::valueOf);
    	registry.register(ExpResearchNumber.class,"exp", ExpResearchNumber::new, ExpResearchNumber::new);
    }
    public static void registerNumberType(Class<? extends IResearchNumber> cls,String type,Function<JsonObject,IResearchNumber> json,Function<PacketBuffer, IResearchNumber> packet) {
    	registry.register(cls, type, json, packet);
    }
    private Numbers() {
    }
    public static void writeId(IResearchNumber e,PacketBuffer pb) {
    	registry.writeId(pb, e);
    }
    public static IResearchNumber deserialize(JsonElement je) {
    	if(je.isJsonPrimitive()) {
    		JsonPrimitive jp=(JsonPrimitive) je;
    		if(jp.isString())
    			return new ExpResearchNumber(jp.getAsString());
    		else if(jp.isNumber())
    			return new ConstResearchNumber(jp.getAsNumber());
    	}
        return registry.deserialize(je.getAsJsonObject());
    }
    public static IResearchNumber deserialize(PacketBuffer data) {
        return registry.read(data);
    }
    public static void writeType(IResearchNumber e,JsonObject jo) {
    	registry.writeType(jo, e);
    }
}
