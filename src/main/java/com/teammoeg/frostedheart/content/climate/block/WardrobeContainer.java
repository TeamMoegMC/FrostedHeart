/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.climate.block.WardrobeBlockEntity;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.event.level.NoteBlockEvent;

public class WardrobeContainer extends CBlockEntityMenu<WardrobeBlockEntity> {

    public WardrobeContainer(int id, Inventory inventoryPlayer, WardrobeBlockEntity tile) {
        super(FHMenuTypes.WARDROBE.get(), tile, id,inventoryPlayer.player, 17);

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
        super.addPlayerInventory(inventoryPlayer, 8, 84, 142);
    }

	@Override
	public void removed(Player pPlayer) {
		super.removed(pPlayer);
		if (pPlayer instanceof ServerPlayer) {
			WardrobeBlock.setOpened(blockEntity.getLevel(),blockEntity.getBlockPos(),blockEntity.getBlockState(),pPlayer,false);
		}
	}
}