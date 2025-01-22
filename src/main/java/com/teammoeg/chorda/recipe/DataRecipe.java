package com.teammoeg.chorda.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.level.Level;

public abstract class DataRecipe extends CustomRecipe {

	public DataRecipe(ResourceLocation pId, CraftingBookCategory pCategory) {
		super(pId, pCategory);
	}

	@Override
	public boolean matches(CraftingContainer pContainer, Level pLevel) {
		return false;
	}

	@Override
	public ItemStack assemble(CraftingContainer pContainer, RegistryAccess pRegistryAccess) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canCraftInDimensions(int pWidth, int pHeight) {
		return false;
	}


}
