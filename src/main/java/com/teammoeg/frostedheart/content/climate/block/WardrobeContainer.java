package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.climate.block.WardrobeBlockEntity;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.event.level.NoteBlockEvent;

public class WardrobeContainer extends CBlockEntityMenu<WardrobeBlockEntity> {

    public WardrobeContainer(int id, Inventory inventoryPlayer, WardrobeBlockEntity tile) {
        super(FHMenuTypes.WARDROBE.get(), tile, id,inventoryPlayer.player, 15);

        for (int j = 0; j < 1; ++j) {
            for (int k = 0; k < 13; ++k) {
                this.addSlot(new Slot(tile, k + j * 5, 44 + k * 18, 19 + j * 18));
            }
        }
//        super.addPlayerInventory(inventoryPlayer, 9, id, id);
        PlayerTemperatureData ptd = PlayerTemperatureData.getCapability(inventoryPlayer.player).resolve().get();

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