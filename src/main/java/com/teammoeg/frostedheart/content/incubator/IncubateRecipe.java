/*
 * Copyright (c) 2022 TeamMoeg
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
 *
 */

package com.teammoeg.frostedheart.content.incubator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;

public class IncubateRecipe extends IESerializableRecipe {
    public static IRecipeType<IncubateRecipe> TYPE;
    public static RegistryObject<IncubateRecipeSerializer> SERIALIZER;

    public IngredientWithSize input;
    public IngredientWithSize catalyst;
    public ItemStack output;
    public FluidStack output_fluid;
    public boolean consume_catalyst;
    public final boolean isFood;
    public int water;
    public int time;



    public IncubateRecipe(ResourceLocation id, IngredientWithSize input,
			IngredientWithSize catalyst, ItemStack output, FluidStack output_fluid, boolean consume_catalyst, int water,
			int time) {
		super(output,TYPE, id);
		this.input = input;
		this.catalyst = catalyst;
		this.output = output;
		this.output_fluid = output_fluid;
		this.consume_catalyst = consume_catalyst;
		this.water = water;
		this.time = time;
		isFood=false;
	}
    public IncubateRecipe() {
		super(ItemStack.EMPTY,TYPE,IncubatorTileEntity.food);
		isFood=true;
		List<IItemProvider> items=new ArrayList<>();
		for(Item i:ForgeRegistries.ITEMS.getValues()) {
			if(i.isFood())
				items.add(i);
		}
		
		this.input = new IngredientWithSize(Ingredient.fromItems(items.toArray(new IItemProvider[0])),1);
		this.catalyst = IngredientWithSize.of(new ItemStack(Items.ROTTEN_FLESH));
		this.output = ItemStack.EMPTY;
		this.output_fluid = new FluidStack(IncubatorTileEntity.getProtein(),25);
		this.consume_catalyst = true;
		this.water = 20;
		this.time = 20;
	}
	@Override
    protected IERecipeSerializer<IncubateRecipe> getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public ItemStack getRecipeOutput() {
        return this.output;
    }

    public static Map<ResourceLocation, IncubateRecipe> recipeList = Collections.emptyMap();
    public static IncubateRecipe findRecipe(ItemStack in,ItemStack catalyst) {
    	return recipeList.values().stream().filter(t->t.input.test(in)).filter(t->t.catalyst==null||t.catalyst.test(catalyst)).findAny().orElse(null);
    }
    public static boolean canBeCatalyst(ItemStack catalyst) {
    	return recipeList.values().stream().filter(r->r.catalyst!=null).anyMatch(r->r.catalyst.testIgnoringSize(catalyst));
    }
    public static boolean canBeInput(ItemStack input) {
    	return recipeList.values().stream().anyMatch(r->r.input.testIgnoringSize(input));
    }
}
