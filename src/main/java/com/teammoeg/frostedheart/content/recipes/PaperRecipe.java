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

package com.teammoeg.frostedheart.content.recipes;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import com.google.common.collect.ImmutableList;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

import java.util.List;

public class PaperRecipe extends IESerializableRecipe {
    public static IRecipeType<PaperRecipe> TYPE;
    public static RegistryObject<IERecipeSerializer<PaperRecipe>> SERIALIZER;
    public Ingredient paper;
    public int maxlevel;
    public static List<PaperRecipe> recipes = ImmutableList.of();

    public PaperRecipe(ResourceLocation id, Ingredient paper, int maxlevel) {
        super(ItemStack.EMPTY, TYPE, id);
        this.paper = paper;
        this.maxlevel = maxlevel;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    protected IERecipeSerializer getIESerializer() {
        return SERIALIZER.get();
    }

}
