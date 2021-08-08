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
import com.teammoeg.frostedheart.recipe.ElectrolyzerRecipe;
import com.teammoeg.frostedheart.recipe.GeneratorRecipe;
import net.minecraft.item.crafting.IRecipeType;

public class FHRecipeTypes {
    public static void registerRecipeTypes() {
        GeneratorRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":generator");
        ElectrolyzerRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":electrolyzer_recipe");
        CrucibleRecipe.TYPE = IRecipeType.register(FHMain.MODID + ":crucible");
    }

}
