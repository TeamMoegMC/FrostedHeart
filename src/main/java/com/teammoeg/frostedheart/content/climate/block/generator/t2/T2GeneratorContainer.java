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

package com.teammoeg.frostedheart.content.climate.block.generator.t2;

import com.teammoeg.chorda.client.ui.Point;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorContainer;

import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.gui.IEContainerMenu.MultiblockMenuContext;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class T2GeneratorContainer extends GeneratorContainer<T2GeneratorState, T2GeneratorLogic> {
    static final Point pin = new Point(29, 63);
    static final Point pout = new Point(112, 55);
    CDataSlot<Integer> slot_tank=CCustomMenuSlot.SLOT_INT.create(this);
    FluidTank fakeTank=new FluidTank(100);
    public T2GeneratorContainer(MenuType<?> type, int id, Inventory inventoryPlayer, MultiblockMenuContext<T2GeneratorState> ctx) {
        super(type, id, inventoryPlayer, ctx);
        slot_tank.bind(()->ctx.mbContext().getState().steamLevel);

    }
    public T2GeneratorContainer(MenuType<?> type, int id, Inventory inventoryPlayer) {
        super(type, id, inventoryPlayer);
        slot_tank.bind(e->fakeTank.setFluid(new FluidStack(FHFluids.STEAM.get(),e)));
    }

    @Override
    public Point getSlotIn() {
        return pin;
    }

    @Override
    public Point getSlotOut() {
        return pout;
    }

    @Override
    public int getTier() {
        return 2;
    }

    @Override
    public IFluidTank getTank() {
        return fakeTank;
    }
	@Override
    public IETemplateMultiblock getNextLevelMultiblock() {
        return null;
    }
	@Override
	protected TemplateMultiblock getMultiblock() {
		return FHMultiblocks.GENERATOR_T2;
	}

}

