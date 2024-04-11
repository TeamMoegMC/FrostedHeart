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

package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.contraptions.components.crafter.MechanicalCraftingRecipe;
import com.teammoeg.frostedheart.content.research.ResearchListeners;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

@Mixin(MechanicalCraftingRecipe.class)
public class MixinMechanicalCraftingRecipe extends ShapedRecipe {


    public MixinMechanicalCraftingRecipe(ResourceLocation idIn, String groupIn, int recipeWidthIn, int recipeHeightIn,
                                         NonNullList<Ingredient> recipeItemsIn, ItemStack recipeOutputIn) {
        super(idIn, groupIn, recipeWidthIn, recipeHeightIn, recipeItemsIn, recipeOutputIn);
    }

    @Inject(at = @At("HEAD"), method = "matches", cancellable = true)
    public void fh$matches(CraftingInventory inv, World worldIn, CallbackInfoReturnable<Boolean> cbi) {
        if (!ResearchListeners.canUseRecipe(ResearchListeners.te, this)) cbi.setReturnValue(false);
    }
}
