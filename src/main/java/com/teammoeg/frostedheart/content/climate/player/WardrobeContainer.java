package com.teammoeg.frostedheart.content.climate.player;

import com.teammoeg.frostedheart.base.menu.FHBlockEntityContainer;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.decoration.RelicChestTileEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WardrobeContainer extends FHBlockEntityContainer<WardrobeBlockEntity> {

    public WardrobeContainer(int id, Inventory inventoryPlayer, PlayerTemperatureData ptd, WardrobeBlockEntity tile) {
        super(FHMenuTypes.WARDROBE.get(), tile, id,inventoryPlayer.player, 15);

        for (int j = 0; j < 1; ++j) {
            for (int k = 0; k < 13; ++k) {
                this.addSlot(new Slot(tile, k + j * 5, 44 + k * 18, 19 + j * 18));
            }
        }
//        super.addPlayerInventory(inventoryPlayer, 9, id, id);

        for(int k=0;k<4;++k) {
            this.addSlot(new Slot(ptd.clothesOfParts.get(PlayerTemperatureData.BodyPart.TORSO), k, 44+k*18, 19+18));
        }

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(inventoryPlayer, i, 8 + i * 18, 142));
    }
}