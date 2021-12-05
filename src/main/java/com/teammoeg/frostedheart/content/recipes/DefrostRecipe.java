package com.teammoeg.frostedheart.content.recipes;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;

public interface DefrostRecipe extends IRecipe<IInventory>{
	Ingredient getIngredient();

	ItemStack[] getIss();
}
