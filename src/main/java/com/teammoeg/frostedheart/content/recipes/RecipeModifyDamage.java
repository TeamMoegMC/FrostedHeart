package com.teammoeg.frostedheart.content.recipes;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.RegistryObject;

public class RecipeModifyDamage extends ShapelessRecipe {
	Ingredient tool;
	Ingredient repair;
	int modify;
	public static RegistryObject<IERecipeSerializer<RecipeModifyDamage>> SERIALIZER;
	public RecipeModifyDamage(ResourceLocation idIn, Ingredient torepair, Ingredient material,int mod) {
		super(idIn, "", torepair.getMatchingStacks()[0], NonNullList.from(Ingredient.EMPTY, torepair, material));
		repair=material;
		tool=torepair;
		modify=mod;
	}

	public boolean matches(CraftingInventory inv, World worldIn) {
		boolean hasArmor = false;
		boolean hasItem = false;
		for (int i = 0; i < inv.getSizeInventory(); ++i) {
			ItemStack itemstack = inv.getStackInSlot(i);
			if (itemstack == null || itemstack.isEmpty()) {
				continue;
			}
			if (repair.test(itemstack)) {
				if (hasItem)
					return false;
				hasItem = true;
			} else {
				if (hasArmor)
					return false;
				if (tool.test(itemstack)) {
					int newdmg = itemstack.getDamage() - modify;
					if (newdmg <= 0 || newdmg > itemstack.getMaxDamage())
						return false;

					hasArmor = true;
				}
			}
		}
		return hasArmor && hasItem;
	}

	/**
	 * Returns an Item that is the result of this recipe
	 */
	public ItemStack getCraftingResult(CraftingInventory inv) {
		for (int j = 0; j < inv.getSizeInventory(); ++j) {
			ItemStack in = inv.getStackInSlot(j);
			if (tool.test(in)) {
				in=in.copy();
				in.setDamage(in.getDamage() - modify);
				return in;
			}
		}

		return ItemStack.EMPTY;
	}

	/**
	 * Used to determine if this recipe can fit in a grid of the given width/height
	 */
	public boolean canFit(int width, int height) {
		return width * height >= 2;
	}

	public IRecipeSerializer<?> getSerializer() {
		return SERIALIZER.get();
	}
}
