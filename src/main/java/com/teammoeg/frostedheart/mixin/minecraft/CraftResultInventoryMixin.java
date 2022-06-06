package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;

import com.teammoeg.frostedheart.data.FHDataManager;
import com.teammoeg.frostedheart.research.ResearchListeners;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.IRecipeHolder;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

@Mixin(CraftResultInventory.class)
public abstract class CraftResultInventoryMixin implements IRecipeHolder, IInventory {

	public CraftResultInventoryMixin() {
	}
	@Override
	public boolean canUseRecipe(World worldIn, ServerPlayerEntity player, IRecipe<?> recipe) {
		if(ResearchListeners.canUseRecipe(player, recipe))
			return IRecipeHolder.super.canUseRecipe(worldIn, player, recipe);
		return false;
	}

}
