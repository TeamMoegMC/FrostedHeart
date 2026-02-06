/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedresearch.mixin.minecraft.research;

import org.spongepowered.asm.mixin.Mixin;

import com.teammoeg.frostedresearch.ResearchHooks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
/**
 * Limit crafting for unresearched item
 * <p>
 * */
@Mixin(ResultContainer.class)
public abstract class ResultContainerMixin_Research implements RecipeHolder, Container {

    public ResultContainerMixin_Research() {
    }

    @Override
    public boolean setRecipeUsed(Level worldIn, ServerPlayer player, Recipe<?> recipe) {
        if (ResearchHooks.canUseRecipe(player, recipe))
            return RecipeHolder.super.setRecipeUsed(worldIn, player, recipe);
        return false;
    }

}
