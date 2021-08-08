package com.teammoeg.frostedheart.container;

import blusunrize.immersiveengineering.common.gui.IEBaseContainer;
import blusunrize.immersiveengineering.common.gui.IESlot;
import blusunrize.immersiveengineering.common.items.IEItems;
import com.teammoeg.frostedheart.recipe.CrucibleRecipe;
import com.teammoeg.frostedheart.tileentity.CrucibleTileEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

public class CrucibleContainer extends IEBaseContainer<CrucibleTileEntity> {
    public CrucibleTileEntity.CrucibleData data;

    public CrucibleContainer(int id, PlayerInventory inventoryPlayer, CrucibleTileEntity tile) {
        super(inventoryPlayer, tile, id);

        // input
        this.addSlot(new IESlot(this, this.inv, 0, 51, 12) {
            @Override
            public boolean isItemValid(ItemStack itemStack) {
                return CrucibleRecipe.findRecipe(itemStack) != null;
            }
        });
        // output
        this.addSlot(new IESlot.Output(this, this.inv, 1, 106, 12));
        // input fuel
        this.addSlot(new IESlot(this, this.inv, 2, 80, 51) {
            @Override
            public boolean isItemValid(ItemStack itemStack) {
                return itemStack.getItem() == IEItems.Ingredients.coalCoke;
            }
        });
        slotCount = 3;

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(inventoryPlayer, i, 8 + i * 18, 142));
        data = tile.guiData;
        trackIntArray(data);
    }
}

