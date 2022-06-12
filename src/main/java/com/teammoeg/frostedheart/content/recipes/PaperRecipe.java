package com.teammoeg.frostedheart.content.recipes;

import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;

public class PaperRecipe extends IESerializableRecipe {

	public PaperRecipe(ItemStack outputDummy, IRecipeType<?> type, ResourceLocation id) {
		super(outputDummy, type, id);
	}

}
