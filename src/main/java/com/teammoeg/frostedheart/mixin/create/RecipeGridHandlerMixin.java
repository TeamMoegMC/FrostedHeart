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

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.util.mixin.IOwnerTile;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;

@Mixin(RecipeGridHandler.class)
public class RecipeGridHandlerMixin {
    @Inject(at = @At("HEAD"), method = "getTargetingCrafter", remap = false)
    private static void fh$getTargetingCrafter(MechanicalCrafterBlockEntity crafter, CallbackInfoReturnable<MechanicalCrafterBlockEntity> cbi) {
        ResearchListeners.te = IOwnerTile.getOwner(crafter);
    }

    @Inject(at = @At("HEAD"), method = "isRecipeAllowed", cancellable = true, remap = false)
    private static void fh$isRecipeAllowed(CraftingRecipe recipe, CraftingContainer inventory, CallbackInfoReturnable<Boolean> cbi) {
        if (!ResearchListeners.canUseRecipe(ResearchListeners.te, recipe))
            cbi.setReturnValue(false);

    }

    @Inject(at = @At("RETURN"), method = "tryToApplyRecipe", cancellable = true, remap = false)
    private static void fh$tryToApplyRecipe(Level world, GroupedItems items, CallbackInfoReturnable<ItemStack> cbi) {
        ResearchListeners.te = null;
    }

    public RecipeGridHandlerMixin() {
    }
}
