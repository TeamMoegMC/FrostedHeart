/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1;

import com.teammoeg.frostedheart.content.climate.heatdevice.generator.MasterGeneratorContainer;
import com.teammoeg.frostedheart.util.client.Point;

import blusunrize.immersiveengineering.common.gui.IEContainerMenu.MultiblockMenuContext;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class T1GeneratorContainer extends MasterGeneratorContainer<T1GeneratorState,T1GeneratorTileEntity> {

    public T1GeneratorContainer(MenuType<?> type, int id, Inventory inventoryPlayer, MultiblockMenuContext<T1GeneratorState> ctx) {
		super(type, id, inventoryPlayer, ctx);
		// TODO Auto-generated constructor stub
	}

	public T1GeneratorContainer(MenuType<?> type, int id, Inventory inventoryPlayer) {
		super(type, id, inventoryPlayer);
		// TODO Auto-generated constructor stub
	}

	static final Point pin=new Point(46,72);
    static final Point pout=new Point(114,72);

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
		return 1;
	}

	@Override
	public FluidTank getTank() {
		return null;
	}


}

