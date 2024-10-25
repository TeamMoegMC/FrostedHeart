package com.teammoeg.frostedheart.data;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.resources.ResourceLocation;

public class DietValueBuilder implements FinishedRecipe {
	final Map<String,Float> groups=new HashMap<>();
	public ResourceLocation out;
	public ResourceLocation rl;

	public DietValueBuilder(ResourceLocation rl,ResourceLocation out) {
		super();
		this.out = out;
		this.rl = rl;
	}
	public void addGroup(int i,float v) {
		// TODO: Change to FH
//		groups.put(DietGroupCodec.groups[i],v);
		
	}
	@Override
	public void serializeRecipeData(JsonObject json) {
		JsonObject jo=new JsonObject();
		groups.entrySet().forEach(e->jo.addProperty(e.getKey(),e.getValue()));
		json.add("groups",jo);
		json.addProperty("item",out.toString());
	}

	@Override
	public ResourceLocation getId() {
		return rl;
	}

	@Override
	public JsonObject serializeAdvancement() {
		return null;
	}

	@Override
	public ResourceLocation getAdvancementId() {
		return null;
	}
	@Override
	public RecipeSerializer<?> getType() {
		// TODO: Change to FH
//		return DietValueRecipe.SERIALIZER.get();
		return null;
	}

}
