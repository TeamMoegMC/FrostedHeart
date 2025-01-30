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

import com.teammoeg.chorda.menu.CBaseMenu;
import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.chorda.menu.slots.ArmorSlot;
import com.teammoeg.chorda.menu.slots.OffHandSlot;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

public class ClothesInventoryMenu extends CBaseMenu {

    public ClothesInventoryMenu(int id, Inventory inventoryPlayer) {
        super(FHMenuTypes.WARDROBE.get(), id,inventoryPlayer.player, 21);

        /*for (int j = 0; j < 1; ++j) {
            for (int k = 0; k < 13; ++k) {
                this.addSlot(new Slot(tile, k + j * 5, 44 + k * 18, 19 + j * 18));
            }
        }*/
//        super.addPlayerInventory(inventoryPlayer, 9, id, id);
        PlayerTemperatureData ptd = PlayerTemperatureData.getCapability(inventoryPlayer.player).resolve().get();
        int y0=6;
        this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.HEAD,inventoryPlayer,39,6,y0));
        this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.CHEST,inventoryPlayer,38,6,y0+18));
        this.addSlot(new OffHandSlot(inventoryPlayer.player,inventoryPlayer,40,6,y0+18*2));
        this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.LEGS,inventoryPlayer,37,6,y0+18*3));
        this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.FEET,inventoryPlayer,36,6,y0+18*4));
        
        for(int k=0;k<4;++k) {
            this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.HEAD,ptd.clothesOfParts.get(PlayerTemperatureData.BodyPart.HEAD), k, 100+k*18, 7));
        }
        for(int k=0;k<4;++k) {
            this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.CHEST,ptd.clothesOfParts.get(PlayerTemperatureData.BodyPart.TORSO), k, 100+k*18, 30));
        }
        for(int k=0;k<4;++k) {
            this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.LEGS,ptd.clothesOfParts.get(PlayerTemperatureData.BodyPart.LEGS), k, 100+k*18, 53));
        }
        for(int k=0;k<4;++k) {
            this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.FEET,ptd.clothesOfParts.get(PlayerTemperatureData.BodyPart.FEET), k, 100+k*18, 76));
        }
        super.addPlayerInventory(inventoryPlayer, 8, 120, 178);
    }
    /**
     * For use by wardrobe
     * */
    ClothesInventoryMenu(int id, Inventory inventoryPlayer,int max_slots) {
        super(FHMenuTypes.WARDROBE.get(), id,inventoryPlayer.player, max_slots);

        /*for (int j = 0; j < 1; ++j) {
            for (int k = 0; k < 13; ++k) {
                this.addSlot(new Slot(tile, k + j * 5, 44 + k * 18, 19 + j * 18));
            }
        }*/
//        super.addPlayerInventory(inventoryPlayer, 9, id, id);
        PlayerTemperatureData ptd = PlayerTemperatureData.getCapability(inventoryPlayer.player).resolve().get();
        int y0=6;
        this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.HEAD,inventoryPlayer,39,6,y0));
        this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.CHEST,inventoryPlayer,38,6,y0+18));
        this.addSlot(new OffHandSlot(inventoryPlayer.player,inventoryPlayer,40,6,y0+18*2));
        this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.LEGS,inventoryPlayer,37,6,y0+18*3));
        this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.FEET,inventoryPlayer,36,6,y0+18*4));
        
        for(int k=0;k<4;++k) {
            this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.HEAD,ptd.clothesOfParts.get(PlayerTemperatureData.BodyPart.HEAD), k, 100+k*18, 7));
        }
        for(int k=0;k<4;++k) {
            this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.CHEST,ptd.clothesOfParts.get(PlayerTemperatureData.BodyPart.TORSO), k, 100+k*18, 30));
        }
        for(int k=0;k<4;++k) {
            this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.LEGS,ptd.clothesOfParts.get(PlayerTemperatureData.BodyPart.LEGS), k, 100+k*18, 53));
        }
        for(int k=0;k<4;++k) {
            this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.FEET,ptd.clothesOfParts.get(PlayerTemperatureData.BodyPart.FEET), k, 100+k*18, 76));
        }
    }

}