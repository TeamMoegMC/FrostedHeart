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

package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.contraptions.components.crafter.MechanicalCrafterTileEntity;
import com.simibubi.create.content.contraptions.components.crafter.RecipeGridHandler;
import com.simibubi.create.content.contraptions.components.crafter.RecipeGridHandler.GroupedItems;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.util.IOwnerTile;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.world.World;

@Mixin(RecipeGridHandler.class)
public class RecipeGridHandlerMixin {
    public RecipeGridHandlerMixin() {
    }

    @Inject(at = @At("HEAD"), method = "getTargetingCrafter", remap = false)
    private static void fh$getTargetingCrafter(MechanicalCrafterTileEntity crafter, CallbackInfoReturnable<MechanicalCrafterTileEntity> cbi) {
        ResearchListeners.te = IOwnerTile.getOwner(crafter);
    }

    @Inject(at = @At("HEAD"), method = "isRecipeAllowed", cancellable = true, remap = false)
    private static void fh$isRecipeAllowed(ICraftingRecipe recipe, CraftingInventory inventory, CallbackInfoReturnable<Boolean> cbi) {
        if (!ResearchListeners.canUseRecipe(ResearchListeners.te, recipe))
            cbi.setReturnValue(false);
        
    }
    @Inject(at = @At("RETURN"), method = "tryToApplyRecipe", cancellable = true, remap = false)
    private static void fh$tryToApplyRecipe(World world, GroupedItems items, CallbackInfoReturnable<ItemStack> cbi) {
		ResearchListeners.te=null;
	}
}
