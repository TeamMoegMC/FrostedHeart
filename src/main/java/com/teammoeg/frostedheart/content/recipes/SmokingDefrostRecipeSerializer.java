package com.teammoeg.frostedheart.content.recipes;

import net.minecraft.network.PacketBuffer;

public class SmokingDefrostRecipeSerializer extends DefrostRecipeSerializer<SmokingDefrostRecipe> {

	public SmokingDefrostRecipeSerializer() {
		super(SmokingDefrostRecipe::new);
	}

	@Override
	public void write(PacketBuffer buffer, SmokingDefrostRecipe recipe) {
		super.write(buffer, recipe);
		buffer.writeFloat(recipe.getExperience());
		buffer.writeVarInt(recipe.getCookTime());
	}

}
