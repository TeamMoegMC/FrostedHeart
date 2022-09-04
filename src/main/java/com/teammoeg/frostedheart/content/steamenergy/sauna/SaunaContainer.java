package com.teammoeg.frostedheart.content.steamenergy.sauna;

import blusunrize.immersiveengineering.common.gui.IEBaseContainer;
import com.teammoeg.frostedheart.content.decoration.RelicChestTileEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

public class SaunaContainer extends IEBaseContainer<SaunaTileEntity> {
    public SaunaContainer(int id, PlayerInventory inventoryPlayer, SaunaTileEntity tile) {
        super(tile, id);
        // medicine slot
        addSlot(new Slot(this.inv, 0, 98, 26) {
            @Override
            public int getSlotStackLimit() {
                return 4;
            }

            @Override
            public boolean isItemValid(ItemStack stack) {
                return SaunaRecipe.findRecipe(stack) != null;
            }
        });

        // player inventory
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        // hotbar
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(inventoryPlayer, i, 8 + i * 18, 142));

        this.slotCount = 1;
        this.tile = tile;
    }
}
