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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import com.teammoeg.frostedheart.FHBaseContainer;
import com.teammoeg.frostedheart.util.client.Point;

import blusunrize.immersiveengineering.common.gui.BlockEntityInventory;
import blusunrize.immersiveengineering.common.gui.IEBaseContainerOld;
import blusunrize.immersiveengineering.common.gui.IESlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public abstract class MasterGeneratorContainer<T extends MasterGeneratorTileEntity<T>> extends FHBaseContainer<T> {
    public ContainerData data;

    public MasterGeneratorContainer(MenuType<?> mt,int id, Inventory inventoryPlayer, T tile) {
        super(mt,tile, id,2);
        Point in=getSlotIn();
        BlockEntityInventory inv=new BlockEntityInventory(tile,this);
        this.addSlot(new IESlot(this, inv, 0, in.getX(), in.getY()) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return tile.isStackValid(0, itemStack);
            }
        });
        Point out=getSlotOut();
        this.addSlot(new IESlot.Output(this, inv, 1, out.getX(), out.getY()));
        this.addPlayerInventory(inventoryPlayer, 8, 140, 198);
        data = tile.guiData;
        addDataSlots(data);
    }
    public abstract Point getSlotIn();
    public abstract Point getSlotOut();
    public abstract int getTier();
    public abstract FluidTank getTank();
}

