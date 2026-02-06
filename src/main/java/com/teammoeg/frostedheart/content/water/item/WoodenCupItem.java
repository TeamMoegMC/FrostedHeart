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

package com.teammoeg.frostedheart.content.water.item;

import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public class WoodenCupItem extends SingleUseSwapDrinkContainerItem {
    public WoodenCupItem(Properties properties, int capacity) {
        super(properties, capacity);
    }

    @Override
	public boolean isValidFluid(FluidStack stack) {

		return super.isValidFluid(stack)&&stack.getFluid().is(FHTags.Fluids.WOODEN_CUP_DRINK.tag);
	}

	@Override
    public Component getName(ItemStack stack) {
        IFluidHandlerItem fluidHandlerItem =  stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        FluidStack fluidStack = fluidHandlerItem.getFluidInTank(0);
        if (fluidStack.isEmpty()) return super.getName(stack);

        Component component = fluidStack.getDisplayName();
        return component.copy().append(Component.translatable("item.frostedheart.wooden_cup_drink"));
    }


	@Override
    public ItemStack getContainerItem() {
        return new ItemStack(FHItems.wooden_cup.get());
    }

    @Override
    public ItemStack getDrinkItem() {
        return new ItemStack(FHItems.wooden_cup_drink.get());
    }
}
