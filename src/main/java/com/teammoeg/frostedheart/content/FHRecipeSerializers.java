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

package com.teammoeg.frostedheart.content;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.recipe.*;
import electrodynamics.common.recipe.categories.fluiditem2fluid.FluidItem2FluidRecipeSerializer;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHRecipeSerializers {
    public static final DeferredRegister<IRecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(
            ForgeRegistries.RECIPE_SERIALIZERS, FHMain.MODID
    );
    public static final IRecipeSerializer<ElectrolyzerRecipe> Electrolyzer_JSON_SERIALIZER = new FluidItem2FluidRecipeSerializer<>(
            ElectrolyzerRecipe.class);

    static {
        GeneratorRecipe.SERIALIZER = RECIPE_SERIALIZERS.register(
                "generator", GeneratorRecipeSerializer::new
        );
        CrucibleRecipe.SERIALIZER = RECIPE_SERIALIZERS.register(
                "crucible", CrucibleRecipeSerializer::new
        );
        ElectrolyzerRecipe.SERIALIZER = RECIPE_SERIALIZERS.register(ElectrolyzerRecipe.RECIPE_GROUP,
                () -> Electrolyzer_JSON_SERIALIZER);
    }
}
