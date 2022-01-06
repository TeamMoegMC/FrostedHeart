package com.teammoeg.frostedheart.loot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;

public class RemoveLootModifier extends LootModifier {
	List<Ingredient> removed=new ArrayList<>();
    private RemoveLootModifier(ILootCondition[] conditionsIn,Collection<Ingredient> pairsin) {
        super(conditionsIn);
        this.removed.addAll(pairsin);
    }

    @Nonnull
    @Override
    protected List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.removeIf(this::shouldRemove);
        return generatedLoot;
    }
    private boolean shouldRemove(ItemStack orig) {
    	for(Ingredient rp:removed) {
    		if(rp.test(orig)) {
    			return true;
    		}
    	}
    	return false;
    }
    public static class Serializer extends GlobalLootModifierSerializer<RemoveLootModifier> {
        @Override
        public RemoveLootModifier read(ResourceLocation location, JsonObject object, ILootCondition[] conditions) {
        	JsonArray ja=object.get("removed").getAsJsonArray();
        	List<Ingredient> changes=new ArrayList<>();
        	for(JsonElement je:ja) {
        		changes.add(Ingredient.deserialize(je));
        	}
            return new RemoveLootModifier(conditions,changes);
        }

        @Override
        public JsonObject write(RemoveLootModifier instance) {
            JsonObject object = new JsonObject();
            JsonArray removed=new JsonArray();
            instance.removed.stream().map(Ingredient::serialize).forEach(removed::add);
            object.add("removed",removed);
            return object;
        }
    }
}
