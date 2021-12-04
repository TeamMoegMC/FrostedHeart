package com.teammoeg.frostedheart.content.recipes;

import java.util.Collections;
import java.util.Map;
import java.util.Random;

import com.teammoeg.frostedheart.content.generator.GeneratorRecipe;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CampfireCookingRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

public class DefrostRecipe extends CampfireCookingRecipe {
	ItemStack[] iss;
	public static RegistryObject<IERecipeSerializer<DefrostRecipe>> SERIALIZER;
	Random recipeRNG=new Random();
	public DefrostRecipe(ResourceLocation p_i50030_1_, String p_i50030_2_, Ingredient p_i50030_3_,
			ItemStack[] results, float p_i50030_5_, int p_i50030_6_) {
		super(p_i50030_1_, p_i50030_2_, p_i50030_3_,ItemStack.EMPTY, p_i50030_5_, p_i50030_6_);
		this.iss=results;
	}
	public Ingredient getIngredient() {
		return super.ingredient;
	}
	@Override
	public boolean isDynamic() {
		return true;
	}

	@Override
	public IRecipeSerializer<?> getSerializer() {
		return SERIALIZER.get();
	}
	public static Map<ResourceLocation,DefrostRecipe> recipeList = Collections.emptyMap();
	@Override
	public ItemStack getCraftingResult(IInventory inv) {
		if(getIss().length<=0)return ItemStack.EMPTY;
		return getIss()[recipeRNG.nextInt(getIss().length)];
	}

	@Override
	public ItemStack getRecipeOutput() {
		return ItemStack.EMPTY;
	}
	public ItemStack[] getIss() {
		return iss;
	}

}
