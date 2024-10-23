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

import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorContainer;
import com.teammoeg.frostedheart.util.FHContainerData;
import com.teammoeg.frostedheart.util.client.Point;

import blusunrize.immersiveengineering.common.gui.IEContainerMenu.MultiblockMenuContext;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class T2GeneratorContainer extends GeneratorContainer<T2GeneratorState, T2GeneratorLogic> {
    static final Point pin = new Point(29, 63);
    static final Point pout = new Point(112, 55);
    FluidTank tank;

    public T2GeneratorContainer(MenuType<?> type, int id, Inventory inventoryPlayer, MultiblockMenuContext<T2GeneratorState> ctx) {
        super(type, id, inventoryPlayer, ctx);
        tank = ctx.mbContext().getState().tank;
        FHContainerData.SLOT_TANK.create(this).bind(tank::getFluid);

    }
    public T2GeneratorContainer(MenuType<?> type, int id, Inventory inventoryPlayer) {
        super(type, id, inventoryPlayer);
        tank = new FluidTank(T2GeneratorState.TANK_CAPACITY);
        FHContainerData.SLOT_TANK.create(this).bind(tank::getFluid, tank::setFluid);

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
    public FluidTank getTank() {
        return tank;
    }


}

