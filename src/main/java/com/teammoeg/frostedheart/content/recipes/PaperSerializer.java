package com.teammoeg.frostedheart.content.recipes;

import com.google.gson.JsonObject;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class PaperSerializer extends IERecipeSerializer<PaperRecipe> {

	public PaperSerializer() {
	}

	@Override
	public PaperRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
		return new PaperRecipe(recipeId,Ingredient.read(buffer),buffer.readVarInt());
	}

	@Override
	public void write(PacketBuffer buffer, PaperRecipe recipe) {
		recipe.paper.write(buffer);
		buffer.writeVarInt(recipe.maxlevel);
	}

	@Override
	public ItemStack getIcon() {
		return new ItemStack(Items.PAPER);
	}

	@Override
	public PaperRecipe readFromJson(ResourceLocation arg0, JsonObject arg1) {
		return new PaperRecipe(arg0,Ingredient.deserialize(arg1.get("item")),arg1.get("level").getAsInt());
	}

}
