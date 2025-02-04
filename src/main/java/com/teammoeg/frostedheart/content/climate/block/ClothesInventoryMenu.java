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

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.menu.CBaseMenu;
import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.chorda.menu.slots.ArmorSlot;
import com.teammoeg.chorda.menu.slots.OffHandSlot;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class ClothesInventoryMenu extends CBaseMenu {
	protected static class LiningSlot extends Slot{
		   public static final ResourceLocation EMPTY_LINING_SLOT_HELMET = FHMain.rl("item/empty_lining_slot_head");
		   public static final ResourceLocation EMPTY_LINING_SLOT_CHESTPLATE = FHMain.rl("item/empty_lining_slot_body");
		   public static final ResourceLocation EMPTY_LINING_SLOT_LEGGINGS = FHMain.rl("item/empty_lining_slot_leg");
		   public static final ResourceLocation EMPTY_LINING_SLOT_BOOTS = FHMain.rl("item/empty_lining_slot_feet");
		   public static final ResourceLocation EMPTY_LINING_SLOT_HAND = FHMain.rl("item/empty_lining_slot_hands");
		public static final ResourceLocation[] TEXTURE_EMPTY_SLOTS = new ResourceLocation[]{EMPTY_LINING_SLOT_CHESTPLATE, EMPTY_LINING_SLOT_LEGGINGS, EMPTY_LINING_SLOT_HAND ,EMPTY_LINING_SLOT_BOOTS,  EMPTY_LINING_SLOT_HELMET};
		Player owner;
		BodyPart part;
		public LiningSlot(Player owner, BodyPart part, Container pContainer, int pSlot, int pX,
				int pY) {
			super(pContainer, pSlot, pX, pY);
			this.owner=owner;
			this.part=part;
		}

		public int getMaxStackSize() {
			return 1;
		}
		public boolean mayPlace(ItemStack p_39746_) {
			return (part.slot.getType()==EquipmentSlot.Type.ARMOR&&p_39746_.canEquip(part.slot, owner))||ArmorTempData.getData(p_39746_, part)!=null;
		}
		public boolean mayPickup(Player p_39744_) {
			ItemStack itemstack = this.getItem();
			return !itemstack.isEmpty() && !p_39744_.isCreative() && EnchantmentHelper.hasBindingCurse(itemstack) ? false
					: super.mayPickup(p_39744_);
		}
		public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
			return Pair.of(InventoryMenu.BLOCK_ATLAS, TEXTURE_EMPTY_SLOTS[part.ordinal()]);
		}
	}
    public ClothesInventoryMenu(int id, Inventory inventoryPlayer,FriendlyByteBuf extraData) {
        this(id,inventoryPlayer);
    }
    public ClothesInventoryMenu(int id, Inventory inventoryPlayer) {
        super(FHMenuTypes.CLOTHES_GUI.get(), id,inventoryPlayer.player, 13);

        createLiningSlots(inventoryPlayer);
        super.addPlayerInventory(inventoryPlayer, 8, 120, 178);
    }
    
    /**
     * For use by wardrobe
     * */
    ClothesInventoryMenu(MenuType<?> type,int id, Inventory inventoryPlayer,int max_slots) {
        super(type, id,inventoryPlayer.player, max_slots);
        createLiningSlots(inventoryPlayer);
    }
    private void createLiningSlots(Inventory inventoryPlayer) {
        PlayerTemperatureData ptd = PlayerTemperatureData.getCapability(inventoryPlayer.player).resolve().get();
        int y0=6;
        this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.HEAD,inventoryPlayer,39,6,y0));
        this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.CHEST,inventoryPlayer,38,6,y0+18));
        this.addSlot(new LiningSlot(inventoryPlayer.player,PlayerTemperatureData.BodyPart.HANDS,ptd.clothesOfParts.get(PlayerTemperatureData.BodyPart.HANDS),40,6,y0+18*2));
        this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.LEGS,inventoryPlayer,37,6,y0+18*3));
        this.addSlot(new ArmorSlot(inventoryPlayer.player,EquipmentSlot.FEET,inventoryPlayer,36,6,y0+18*4));
        
        for(int k=0;k<1;++k) {
            this.addSlot(new LiningSlot(inventoryPlayer.player,PlayerTemperatureData.BodyPart.HEAD,ptd.clothesOfParts.get(PlayerTemperatureData.BodyPart.HEAD), k, 100+k*18, 7));
        }
        for(int k=0;k<3;++k) {
            this.addSlot(new LiningSlot(inventoryPlayer.player,PlayerTemperatureData.BodyPart.TORSO,ptd.clothesOfParts.get(PlayerTemperatureData.BodyPart.TORSO), k, 100+k*18, 30));
        }
        for(int k=0;k<3;++k) {
            this.addSlot(new LiningSlot(inventoryPlayer.player,PlayerTemperatureData.BodyPart.LEGS,ptd.clothesOfParts.get(PlayerTemperatureData.BodyPart.LEGS), k, 100+k*18, 53));
        }
        for(int k=0;k<1;++k) {
            this.addSlot(new LiningSlot(inventoryPlayer.player,PlayerTemperatureData.BodyPart.FEET,ptd.clothesOfParts.get(PlayerTemperatureData.BodyPart.FEET), k, 100+k*18, 76));
        }
    }
    

}