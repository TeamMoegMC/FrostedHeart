package com.teammoeg.frostedheart.content.recipes;

import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class DietValueSerializer extends IERecipeSerializer<DietValueRecipe> {

	@Override
	public DietValueRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
		return new DietValueRecipe(recipeId,DietGroupCodec.read(buffer),buffer.readRegistryId());
	}

	@Override
	public void write(PacketBuffer buffer, DietValueRecipe recipe) {
		DietGroupCodec.write(buffer, recipe.groups);
		buffer.writeRegistryId(recipe.item);
	}

	@Override
	public ItemStack getIcon() {
		return ItemStack.EMPTY;
	}

	@Override
	public DietValueRecipe readFromJson(ResourceLocation id, JsonObject json) {
		Map<String,Float> m=json.get("groups").getAsJsonObject().entrySet().stream().collect(Collectors.toMap(e->e.getKey(),e->e.getValue().getAsFloat()));
		Item i=ForgeRegistries.ITEMS.getValue(new ResourceLocation(json.get("item").getAsString()));
		if(i==null||i==Items.AIR)
			return null;
		return new DietValueRecipe(id,m,i);
	}

}
