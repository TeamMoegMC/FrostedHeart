package com.teammoeg.frostedheart.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.LootConditionType;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.conditions.ILootCondition;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

public class ModLootCondition implements ILootCondition {
    public static LootConditionType TYPE;
    private Set<String> mods=new HashSet<>();
    public ModLootCondition(String mod) {
    	mods.add(mod);
    	
    }
    public ModLootCondition(Collection<String> mods) {
    	mods.addAll(mods);
    	
    }
    @SuppressWarnings("resource")
    @Override
    public boolean test(LootContext t) {
        return mods.contains(t.getQueriedLootTableId().getNamespace());
    }

    @Override
    public LootConditionType getConditionType() {
        return TYPE;
    }

    public static class Serializer implements ILootSerializer<ModLootCondition> {

        @Override
        public void serialize(JsonObject jsonObject, ModLootCondition cond, JsonSerializationContext serializationContext) {
        	if(cond.mods.size()==1)
        		jsonObject.addProperty("mod",cond.mods.iterator().next());
        	else
        		jsonObject.add("mods",SerializeUtil.toJsonList(cond.mods,JsonPrimitive::new));
        }

        @Nonnull
        @Override
        public ModLootCondition deserialize(JsonObject jsonObject, JsonDeserializationContext context) {
        	if(jsonObject.has("mod")) {
        		return new ModLootCondition(jsonObject.get("mod").getAsString());
        	}
			return new ModLootCondition(SerializeUtil.parseJsonElmList(jsonObject.get("mods"),JsonElement::getAsString));
        }
    }
}
