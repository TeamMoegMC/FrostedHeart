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

package com.teammoeg.frostedheart.mixin.tetra;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import blusunrize.immersiveengineering.api.IEApi;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.items.modular.impl.toolbelt.ModularToolbeltItem;
import se.mickelus.tetra.items.modular.impl.toolbelt.inventory.ToolbeltInventory;

@Mixin(ToolbeltInventory.class)
public abstract class MixinToolbeltInventory  implements Container{

	public MixinToolbeltInventory() {
	}
	@Shadow(remap=false)
	protected Predicate<ItemStack> predicate;
	
	/**
	 * @author khjxiaogu
	 * @reason
	 */
	@Overwrite
	@Override
	public boolean canPlaceItem(int pIndex, ItemStack pStack) {
		return (!pStack.is(ModularToolbeltItem.instance.get()) && this.predicate.test(pStack)&&IEApi.isAllowedInCrate(pStack));
	}

	/**
	 * @author khjxiaogu
	 * @reason
	 */
	@Overwrite(remap=false)
	public boolean isItemValid(ItemStack itemStack) {
		return (!itemStack.is(ModularToolbeltItem.instance.get())&&(!itemStack.hasTag()||itemStack.getItem().getMaxStackSize()==1) && this.predicate.test(itemStack)&&IEApi.isAllowedInCrate(itemStack));
	}
}
