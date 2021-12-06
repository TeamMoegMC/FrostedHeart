/*
 * Copyright (c) 2021 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.content.recipes;

import java.util.Collections;
import java.util.Map;

import com.google.common.base.Optional;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import net.minecraft.entity.MobEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.RegistryObject;

public class RecipeInner extends SpecialRecipe {
	public static RegistryObject<IERecipeSerializer<RecipeInner>> SERIALIZER;

	protected RecipeInner(ResourceLocation id, Ingredient t, int d) {
		super(id);
		type = t;
		durability = d;
	}

	public int getDurability() {
		return durability;
	}

	public ResourceLocation getBuffType() {
		return Optional.fromNullable(type.getMatchingStacks()[0]).transform(e -> e.getItem().getRegistryName())
				.or(new ResourceLocation("minecraft", "air"));
	}

	public Ingredient getIngredient() {
		return type;
	}

	Ingredient type;
	int durability;

	/**
	 * Used to check if a recipe matches current crafting inventory
	 */
	public boolean matches(CraftingInventory inv, World worldIn) {
		boolean hasArmor = false;
		boolean hasItem = false;
		for (int i = 0; i < inv.getSizeInventory(); ++i) {
			ItemStack itemstack = inv.getStackInSlot(i);
			if (itemstack == null || itemstack.isEmpty()) {
				continue;
			}
			if (type.test(itemstack)) {
				if (hasItem)
					return false;
				hasItem = true;
			} else {
				if (hasArmor)
					return false;
				EquipmentSlotType type = MobEntity.getSlotForItemStack(itemstack);
				if (type != null && type != EquipmentSlotType.MAINHAND && type != EquipmentSlotType.OFFHAND)
					if(itemstack.hasTag()) {
						if(!itemstack.getTag().getString("inner_cover").isEmpty())return false;
					}
					hasArmor = true;
			}
		}
		return hasArmor && hasItem;
	}

	public boolean matches(ItemStack itemstack) {
		EquipmentSlotType type = MobEntity.getSlotForItemStack(itemstack);
		return type != null && type != EquipmentSlotType.MAINHAND && type != EquipmentSlotType.OFFHAND;
	}

	/**
	 * Returns an Item that is the result of this recipe
	 */
	public ItemStack getCraftingResult(CraftingInventory inv) {
		ItemStack buffstack = ItemStack.EMPTY;
		ItemStack armoritem = ItemStack.EMPTY;
		for (int i = 0; i < inv.getSizeInventory(); ++i) {
			ItemStack itemstack = inv.getStackInSlot(i);
			if (itemstack != null && !itemstack.isEmpty()) {
				if (type.test(itemstack)) {
					if (!buffstack.isEmpty())
						return ItemStack.EMPTY;
					buffstack = itemstack;
				} else {
					if (!armoritem.isEmpty())
						return ItemStack.EMPTY;
					EquipmentSlotType type = MobEntity.getSlotForItemStack(itemstack);
					if (type != null && type != EquipmentSlotType.MAINHAND && type != EquipmentSlotType.OFFHAND)
						if(itemstack.hasTag()) {
							if(!itemstack.getTag().getString("inner_cover").isEmpty())return ItemStack.EMPTY;
						}
						armoritem = itemstack;
				}
			}
		}

		if (!armoritem.isEmpty() && !buffstack.isEmpty()) {
			ItemStack ret = armoritem.copy();
			ItemNBTHelper.putString(ret, "inner_cover", buffstack.getItem().getRegistryName().toString());
			CompoundNBT nbt = buffstack.getTag();
			ret.getTag().put("inner_cover_tag", nbt != null ? nbt : new CompoundNBT());
			return ret;
		}
		return ItemStack.EMPTY;
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv) {
		NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
		return nonnulllist;
	}

	public static Map<ResourceLocation, RecipeInner> recipeList = Collections.emptyMap();

	/**
	 * Used to determine if this recipe can fit in a grid of the given width/height
	 */
	public boolean canFit(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public IRecipeSerializer<?> getSerializer() {
		return SERIALIZER.get();
	}

}