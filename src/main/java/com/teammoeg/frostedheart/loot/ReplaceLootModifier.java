package com.teammoeg.frostedheart.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ReplaceLootModifier extends LootModifier {
	static class ReplacePair{
		Ingredient from;
		Item to;
		public ReplacePair(Ingredient from, Item to) {
			this.from = from;
			this.to = to;
		}
		JsonObject toJson() {
			JsonObject jo=new JsonObject();
			jo.add("from",from.serialize());
			jo.addProperty("to",to.getRegistryName().toString());
			return jo;
		}
	}
	List<ReplacePair> remap=new ArrayList<>();
    private ReplaceLootModifier(ILootCondition[] conditionsIn,Collection<ReplacePair> pairsin) {
        super(conditionsIn);
        this.remap.addAll(pairsin);
    }

    @Nonnull
    @Override
    protected List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
	    generatedLoot.replaceAll(this::doReplace);
        return generatedLoot;
    }
    private ItemStack doReplace(ItemStack orig) {
    	for(ReplacePair rp:remap) {
    		if(rp.from.test(orig)) {
    			return new ItemStack(rp.to,orig.getCount());
    		}
    	}
    	return orig;
    }
    public static class Serializer extends GlobalLootModifierSerializer<ReplaceLootModifier> {
        @Override
        public ReplaceLootModifier read(ResourceLocation location, JsonObject object, ILootCondition[] conditions) {
        	JsonArray ja=object.get("changes").getAsJsonArray();
        	List<ReplacePair> changes=new ArrayList<>();
        	for(JsonElement je:ja) {
        		if(je.isJsonObject()) {
        			changes.add(new ReplacePair(Ingredient.deserialize(je.getAsJsonObject().get("from")),ForgeRegistries.ITEMS.getValue(new ResourceLocation(je.getAsJsonObject().get("to").getAsString()))));
        		}
        	}
            return new ReplaceLootModifier(conditions,changes);
        }

        @Override
        public JsonObject write(ReplaceLootModifier instance) {
            JsonObject object = new JsonObject();
            JsonArray changes=new JsonArray();
            instance.remap.stream().map(ReplacePair::toJson).forEach(changes::add);
            object.add("changes",changes);
            return object;
        }
    }
}
