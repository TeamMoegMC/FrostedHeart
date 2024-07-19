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

package com.teammoeg.frostedheart.data;

import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorRecipe;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.crafting.builders.IEFinishedRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ITag;

public class GeneratorRecipeBuilder extends IEFinishedRecipe<GeneratorRecipeBuilder> {
    private GeneratorRecipeBuilder() {
        super(GeneratorRecipe.SERIALIZER.get());
    }

    public static GeneratorRecipeBuilder builder(Item result) {
        return new GeneratorRecipeBuilder().addResult(result);
    }

    public static GeneratorRecipeBuilder builder(ItemStack result) {
        return new GeneratorRecipeBuilder().addResult(result);
    }

    public static GeneratorRecipeBuilder builder(ITag<Item> result, int count) {
        return new GeneratorRecipeBuilder().addResult(new IngredientWithSize(result, count));
    }
}
