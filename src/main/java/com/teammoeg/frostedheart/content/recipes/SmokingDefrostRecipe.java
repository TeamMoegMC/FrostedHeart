package com.teammoeg.frostedheart.content.recipes;

import java.util.Collections;
import java.util.Map;
import java.util.Random;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.SmokingRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

public class SmokingDefrostRecipe extends SmokingRecipe implements DefrostRecipe{

	public static RegistryObject<IRecipeSerializer<SmokingDefrostRecipe>> SERIALIZER;
	public static Map<ResourceLocation,DefrostRecipe> recipeList = Collections.emptyMap();
	@Override
	public IRecipeSerializer<?> getSerializer() {
		return SERIALIZER.get();
	}
	ItemStack[] iss;
	
	Random recipeRNG=new Random();
	public SmokingDefrostRecipe(ResourceLocation p_i50030_1_, String p_i50030_2_, Ingredient p_i50030_3_,
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
	public ItemStack getCraftingResult(IInventory inv) {
		if(iss.length<=0)return ItemStack.EMPTY;
		return iss[recipeRNG.nextInt(getIss().length)].copy();
	}

	@Override
	public ItemStack getRecipeOutput() {
		//ItemStack is=DistExecutor.unsafeCallWhenOn(Dist.CLIENT,()->(()->new ItemStack(FHItems.random_seeds)));
		//if(is==null)
			
		
		return getCraftingResult(null);
	}
	public ItemStack[] getIss() {
		return iss;
	}
}
