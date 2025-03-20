/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedresearch.mixin.immersiveengineering;

import java.util.UUID;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedresearch.ResearchListeners;
import com.teammoeg.frostedresearch.mixinutil.IOwnerTile;

import blusunrize.immersiveengineering.common.blocks.metal.CrafterPatternInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.core.NonNullList;

@Mixin(CrafterPatternInventory.class)
public class CrafterPatternInventoryMixin implements IOwnerTile{
    @Shadow(remap = false)
    public NonNullList<ItemStack> inv;
    @SuppressWarnings("rawtypes")
    @Shadow(remap = false)
    public Recipe recipe;
    private UUID owner;
    public CrafterPatternInventoryMixin() {
    }
    
    @Inject(at=@At("TAIL"),method="recalculateOutput",remap=false)
	public void fh$recalculateOutput(@Nullable Level level,CallbackInfo cbi)
	{
        if (level.isClientSide) {
            if (!ResearchListeners.canUseRecipe(recipe))
            	removeRecipe();
        } else if (!ResearchListeners.canUseRecipe(owner, recipe))
        	removeRecipe();

	}
   private void removeRecipe() {
		this.recipe = null;
		this.inv.set(9, ItemStack.EMPTY);
   }

	@Override
	public UUID getStoredOwner() {
		return owner;
	}
	
	@Override
	public void setStoredOwner(UUID id) {
		owner=id;
		
	}

}
