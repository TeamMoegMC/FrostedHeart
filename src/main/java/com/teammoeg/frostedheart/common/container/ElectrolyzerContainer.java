package com.teammoeg.frostedheart.common.container;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.common.tile.ElectrolyzerTile;
import electrodynamics.prefab.inventory.container.GenericContainer;
import electrodynamics.prefab.inventory.container.slot.GenericSlot;
import electrodynamics.prefab.inventory.container.slot.SlotRestricted;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IntArray;

public class ElectrolyzerContainer extends GenericContainer<ElectrolyzerTile> {
    public ElectrolyzerContainer(int id, PlayerInventory playerinv) {
        this(id, playerinv, new Inventory(4), new IntArray(3));
    }

    public ElectrolyzerContainer(int id, PlayerInventory playerinv, IInventory inventory, IIntArray inventorydata) {
        super(FHTileTypes.ELECTROLYZER_CONTAINER.get(), id, playerinv, inventory, inventorydata);
    }

    public ElectrolyzerContainer(ContainerType<?> type, int id, PlayerInventory playerinv, IInventory inventory, IIntArray inventorydata) {
        super(type, id, playerinv, inventory, inventorydata);
    }

    @Override
    public void addInventorySlots(IInventory inv, PlayerInventory playerinv) {
        addSlot(new GenericSlot(inv, nextIndex(), 83, 31));
        addSlot(new GenericSlot(inv, nextIndex(), 108, 31));
        addSlot(new SlotRestricted(inv, nextIndex(), 31, 51, ElectrolyzerTile.SUPPORTED_INPUT_FLUIDS));
        addSlot(new SlotRestricted(inv, nextIndex(), 31, 31, SlotRestricted.VALID_EMPTY_BUCKETS[1]));
    }
}
