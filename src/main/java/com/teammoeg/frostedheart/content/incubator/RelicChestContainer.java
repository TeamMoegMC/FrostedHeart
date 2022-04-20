package com.teammoeg.frostedheart.content.incubator;

import blusunrize.immersiveengineering.common.gui.IEBaseContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;

public class RelicChestContainer extends IEBaseContainer<RelicChestTileEntity> {
    public RelicChestContainer(int id, PlayerInventory inventoryPlayer, RelicChestTileEntity tile) {
        super(tile, id);

        for(int j = 0; j < 3; ++j) {
            for(int k = 0; k < 5; ++k) {
                this.addSlot(new Slot(this.inv, k + j * 5, 44 + k * 18, 19 + j * 18));
            }
        }
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(inventoryPlayer, i, 8 + i * 18, 142));
        this.slotCount=15;
        this.tile = tile;
    }
}
