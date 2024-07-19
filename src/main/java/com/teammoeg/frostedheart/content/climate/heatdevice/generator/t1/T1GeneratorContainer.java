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

import net.minecraft.entity.player.PlayerInventory;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class T1GeneratorContainer extends MasterGeneratorContainer<T1GeneratorTileEntity> {
    public T1GeneratorContainer(int id, PlayerInventory inventoryPlayer, T1GeneratorTileEntity tile) {
		super(id, inventoryPlayer, tile);
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

