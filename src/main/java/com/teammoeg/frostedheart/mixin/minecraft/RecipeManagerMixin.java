package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.Map;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.teammoeg.frostedheart.research.ResearchListeners;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
	@Overwrite
	public <C extends IInventory, T extends IRecipe<C>> Optional<T> getRecipe(IRecipeType<T> recipeTypeIn,
			C inventoryIn, World worldIn) {
		if(recipeTypeIn==IRecipeType.CRAFTING&&ForgeHooks.getCraftingPlayer()!=null) {
			return this.getRecipes(recipeTypeIn).values().stream().flatMap((recipe) -> {
				return Util.streamOptional(recipeTypeIn.matches(recipe, worldIn, inventoryIn));
			}).filter(t->ResearchListeners.canUseRecipe(ForgeHooks.getCraftingPlayer(), t)).findFirst();
		}
		return this.getRecipes(recipeTypeIn).values().stream().flatMap((recipe) -> {
			return Util.streamOptional(recipeTypeIn.matches(recipe, worldIn, inventoryIn));
		}).findFirst();
	}

	@Shadow
	abstract <C extends IInventory, T extends IRecipe<C>> Map<ResourceLocation, IRecipe<C>> getRecipes(IRecipeType<T> recipeTypeIn);
}
