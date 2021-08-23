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
import com.teammoeg.frostedheart.recipe.CrucibleRecipe;
import com.teammoeg.frostedheart.recipe.CrucibleRecipeSerializer;
import com.teammoeg.frostedheart.recipe.GeneratorRecipe;
import com.teammoeg.frostedheart.recipe.GeneratorRecipeSerializer;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHRecipeSerializers {
    public static final DeferredRegister<IRecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(
            ForgeRegistries.RECIPE_SERIALIZERS, FHMain.MODID
    );
    static {
        GeneratorRecipe.SERIALIZER = RECIPE_SERIALIZERS.register(
                "generator", GeneratorRecipeSerializer::new
        );
        CrucibleRecipe.SERIALIZER = RECIPE_SERIALIZERS.register(
                "crucible", CrucibleRecipeSerializer::new
        );
    }
}
