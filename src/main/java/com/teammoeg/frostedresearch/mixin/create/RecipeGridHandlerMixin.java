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

package com.teammoeg.frostedresearch.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import com.teammoeg.frostedresearch.ResearchHooks;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;

@Mixin(RecipeGridHandler.class)
public class RecipeGridHandlerMixin {
    @Inject(at = @At("HEAD"), method = "isRecipeAllowed", cancellable = true, remap = false)
    private static void fh$isRecipeAllowed(CraftingRecipe recipe, CraftingContainer inventory, CallbackInfoReturnable<Boolean> cbi) {
        if (!ResearchHooks.canUseRecipe(ResearchHooks.currentRecipeOwner(), recipe))
            cbi.setReturnValue(false);

    }

    public RecipeGridHandlerMixin() {
    }
}
