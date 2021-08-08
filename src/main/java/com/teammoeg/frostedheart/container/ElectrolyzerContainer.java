/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.container;

import com.teammoeg.frostedheart.content.FHContainers;
import com.teammoeg.frostedheart.tileentity.ElectrolyzerTileEntity;
import electrodynamics.common.tile.TileChemicalMixer;
import electrodynamics.prefab.inventory.container.GenericContainer;
import electrodynamics.prefab.inventory.container.slot.GenericSlot;
import electrodynamics.prefab.inventory.container.slot.SlotRestricted;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IntArray;

public class ElectrolyzerContainer extends GenericContainer<ElectrolyzerTileEntity> {
    public ElectrolyzerContainer(int id, PlayerInventory playerinv) {
        this(id, playerinv, new Inventory(4), new IntArray(3));
    }

    public ElectrolyzerContainer(int id, PlayerInventory playerinv, IInventory inventory, IIntArray inventorydata) {
        super(FHContainers.ELECTROLYZER_CONTAINER.get(), id, playerinv, inventory, inventorydata);
    }

    public ElectrolyzerContainer(ContainerType<?> type, int id, PlayerInventory playerinv, IInventory inventory, IIntArray inventorydata) {
        super(type, id, playerinv, inventory, inventorydata);
    }

    @Override
    public void addInventorySlots(IInventory inv, PlayerInventory playerinv) {
        addSlot(new GenericSlot(inv, nextIndex(), 60, 31));
        addSlot(new SlotRestricted(inv, nextIndex(), 40, 51, TileChemicalMixer.SUPPORTED_INPUT_FLUIDS));
        addSlot(new SlotRestricted(inv, nextIndex(), 60, 51, SlotRestricted.VALID_EMPTY_BUCKETS[1]));
    }
}
