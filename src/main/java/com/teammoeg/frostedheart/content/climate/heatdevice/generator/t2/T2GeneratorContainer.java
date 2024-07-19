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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2;

import com.teammoeg.frostedheart.content.climate.heatdevice.generator.MasterGeneratorContainer;
import com.teammoeg.frostedheart.util.client.Point;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class T2GeneratorContainer extends MasterGeneratorContainer<T2GeneratorTileEntity> {

	public T2GeneratorContainer(int id, PlayerInventory inventoryPlayer, T2GeneratorTileEntity tile) {
		super(id, inventoryPlayer, tile);
	}
    static final Point pin=new Point(29,63);
    static final Point pout=new Point(112,55);
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
	public FluidTank getTank() {
		return super.tile.tank;
	}



}

