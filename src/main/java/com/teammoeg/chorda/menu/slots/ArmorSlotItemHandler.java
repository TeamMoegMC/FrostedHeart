/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.chorda.menu.slots;

import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 基于 {@link IItemHandler} 的盔甲装备槽，继承 {@link SlotItemHandler}，用于在使用Forge物品处理器的自定义菜单中管理盔甲装备。
 * <p>
 * Armor equipment slot based on {@link IItemHandler}, extending {@link SlotItemHandler}, used to manage armor equipment
 * in custom menus that use Forge item handlers. Handles equipment sound effects, binding curse restrictions,
 * and displays the appropriate empty slot icon.
 *
 * @see ArmorSlot
 */
public class ArmorSlotItemHandler extends SlotItemHandler {
	/** 各盔甲槽位的空槽位纹理 / Empty slot textures for each armor slot type */
	public static final ResourceLocation[] TEXTURE_EMPTY_SLOTS = new ResourceLocation[]{InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET};
	/** 拥有此槽位的玩家 / The player who owns this slot */
	protected Player owner;
	/** 此槽位对应的装备类型 / The equipment slot type this slot corresponds to */
	protected EquipmentSlot equipmentslot;


	/**
	 * 构造一个基于物品处理器的盔甲槽。
	 * <p>
	 * Constructs an armor slot backed by an item handler.
	 *
	 * @param owner 拥有此槽位的玩家 / The player who owns this slot
	 * @param equipmentslot 装备槽位类型 / The equipment slot type
	 * @param pContainer 物品处理器 / The item handler
	 * @param pSlot 槽位索引 / The slot index
	 * @param pX 槽位X坐标 / The slot X position
	 * @param pY 槽位Y坐标 / The slot Y position
	 */
	public ArmorSlotItemHandler(Player owner, EquipmentSlot equipmentslot,IItemHandler pContainer, int pSlot, int pX, int pY) {
		super(pContainer, pSlot, pX, pY);
		this.owner = owner;
		this.equipmentslot = equipmentslot;
	}

	/**
	 * 当玩家设置物品时触发装备音效。
	 * <p>
	 * Triggers equipment sound effects when a player sets an item.
	 *
	 * @param p_270969_ 要设置的物品堆 / The item stack to set
	 */
	public void setByPlayer(ItemStack p_270969_) {
		onEquipItem(owner, equipmentslot, p_270969_, this.getItem());
		super.setByPlayer(p_270969_);
	}

	/**
	 * 处理装备穿戴事件，触发装备音效。
	 * <p>
	 * Handles the equip event and triggers equipment sound effects.
	 *
	 * @param pPlayer 玩家 / The player
	 * @param pSlot 装备槽位类型 / The equipment slot type
	 * @param pNewItem 新装备的物品 / The new item being equipped
	 * @param pOldItem 旧装备的物品 / The old item being replaced
	 */
	void onEquipItem(Player pPlayer, EquipmentSlot pSlot, ItemStack pNewItem, ItemStack pOldItem) {
		Equipable equipable = Equipable.get(pNewItem);
		if (equipable != null) {
			pPlayer.onEquipItem(pSlot, pOldItem, pNewItem);
		}

	}

	/**
	 * 返回此槽位的最大堆叠数量，盔甲槽固定为1。
	 * <p>
	 * Returns the maximum stack size for this slot. Armor slots always return 1.
	 *
	 * @return 最大堆叠数量（1） / The maximum stack size (1)
	 */
	public int getMaxStackSize() {
		return 1;
	}

	/**
	 * 检查物品是否可以放入此槽位，仅允许匹配的盔甲类型。
	 * <p>
	 * Checks if the item stack can be placed in this slot. Only allows items that match the armor slot type.
	 *
	 * @param p_39746_ 要放置的物品堆 / The item stack to place
	 * @return 物品是否可以装备到此槽位 / Whether the item can be equipped in this slot
	 */
	public boolean mayPlace(ItemStack p_39746_) {
		return p_39746_.canEquip(equipmentslot, owner);
	}

	/**
	 * 检查玩家是否可以从此槽位取出物品。带有绑定诅咒的物品在非创造模式下无法取出。
	 * <p>
	 * Checks if the player can pick up the item from this slot. Items with binding curse cannot be removed in non-creative mode.
	 *
	 * @param p_39744_ 尝试取出物品的玩家 / The player attempting to pick up the item
	 * @return 是否允许取出 / Whether pickup is allowed
	 */
	public boolean mayPickup(Player p_39744_) {
		ItemStack itemstack = this.getItem();
		return !itemstack.isEmpty() && !p_39744_.isCreative() && EnchantmentHelper.hasBindingCurse(itemstack) ? false
				: super.mayPickup(p_39744_);
	}

	/**
	 * 返回槽位为空时显示的图标，根据装备类型显示对应的盔甲轮廓图标。
	 * <p>
	 * Returns the icon to display when the slot is empty. Shows the appropriate armor outline icon based on the equipment type.
	 *
	 * @return 空槽位图标的图集位置和纹理位置对 / A pair of atlas location and texture location for the empty slot icon
	 */
	public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
		return Pair.of(InventoryMenu.BLOCK_ATLAS, TEXTURE_EMPTY_SLOTS[equipmentslot.getIndex()]);
	}

}
