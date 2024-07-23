/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;

import com.teammoeg.frostedheart.content.research.ResearchListeners;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.IRecipeHolder;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
/**
 * Limit crafting for unresearched item
 * <p>
 * */
@Mixin(CraftResultInventory.class)
public abstract class CraftResultInventoryMixin implements IRecipeHolder, IInventory {

    public CraftResultInventoryMixin() {
    }

    @Override
    public boolean setRecipeUsed(World worldIn, ServerPlayerEntity player, IRecipe<?> recipe) {
        if (ResearchListeners.canUseRecipe(player, recipe))
            return IRecipeHolder.super.setRecipeUsed(worldIn, player, recipe);
        return false;
    }

}
