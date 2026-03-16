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
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 副手装备槽，继承 {@link ArmorSlot}，用于在自定义菜单中显示和管理副手物品。
 * <p>
 * Off-hand equipment slot extending {@link ArmorSlot}, used to display and manage off-hand items in custom menus.
 * Unlike armor slots, this slot accepts any item and uses the shield icon as the empty slot indicator.
 */
public class OffHandSlot extends ArmorSlot {



	/**
	 * 构造一个副手槽位。
	 * <p>
	 * Constructs an off-hand slot.
	 *
	 * @param owner 拥有此槽位的玩家 / The player who owns this slot
	 * @param pContainer 容器 / The container
	 * @param pSlot 槽位索引 / The slot index
	 * @param pX 槽位X坐标 / The slot X position
	 * @param pY 槽位Y坐标 / The slot Y position
	 */
	public OffHandSlot(Player owner, Container pContainer, int pSlot, int pX, int pY) {
		super(owner, EquipmentSlot.OFFHAND, pContainer, pSlot, pX, pY);
	}

	/**
	 * 返回此槽位的最大堆叠数量，使用容器的默认最大堆叠数。
	 * <p>
	 * Returns the maximum stack size for this slot, using the container's default max stack size.
	 *
	 * @return 容器的最大堆叠数量 / The container's maximum stack size
	 */
	@Override
	public int getMaxStackSize() {
		return container.getMaxStackSize();
	}

	/**
	 * 副手槽允许放入任何物品。
	 * <p>
	 * The off-hand slot allows any item to be placed.
	 *
	 * @param p_39746_ 要放置的物品堆 / The item stack to place
	 * @return 始终返回true / Always returns true
	 */
	@Override
	public boolean mayPlace(ItemStack p_39746_) {
		return true;
	}

	/**
	 * 副手槽允许任何玩家取出物品。
	 * <p>
	 * The off-hand slot allows any player to pick up items.
	 *
	 * @param p_39744_ 尝试取出物品的玩家 / The player attempting to pick up the item
	 * @return 始终返回true / Always returns true
	 */
	@Override
	public boolean mayPickup(Player p_39744_) {
		return true;
	}

	/**
	 * 返回槽位为空时显示的盾牌图标。
	 * <p>
	 * Returns the shield icon to display when the slot is empty.
	 *
	 * @return 空槽位盾牌图标的图集位置和纹理位置对 / A pair of atlas location and texture location for the empty shield icon
	 */
	@Override
	public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
		return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
	}

}
