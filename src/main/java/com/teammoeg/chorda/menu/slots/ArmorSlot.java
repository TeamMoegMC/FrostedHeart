package com.teammoeg.chorda.menu.slots;

import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class ArmorSlot extends Slot {
	public static final ResourceLocation[] TEXTURE_EMPTY_SLOTS = new ResourceLocation[]{InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET};
	protected Player owner;
	protected EquipmentSlot equipmentslot;


	public ArmorSlot(Player owner, EquipmentSlot equipmentslot,Container pContainer, int pSlot, int pX, int pY) {
		super(pContainer, pSlot, pX, pY);
		this.owner = owner;
		this.equipmentslot = equipmentslot;
	}

	public void setByPlayer(ItemStack p_270969_) {
		onEquipItem(owner, equipmentslot, p_270969_, this.getItem());
		super.setByPlayer(p_270969_);
	}

	void onEquipItem(Player pPlayer, EquipmentSlot pSlot, ItemStack pNewItem, ItemStack pOldItem) {
		Equipable equipable = Equipable.get(pNewItem);
		if (equipable != null) {
			pPlayer.onEquipItem(pSlot, pOldItem, pNewItem);
		}

	}

	/**
	 * Returns the maximum stack size for a given slot (usually the same as
	 * getInventoryStackLimit(), but 1 in
	 * the case of armor slots)
	 */
	public int getMaxStackSize() {
		return 1;
	}

	/**
	 * Check if the stack is allowed to be placed in this slot, used for armor slots
	 * as well as furnace fuel.
	 */
	public boolean mayPlace(ItemStack p_39746_) {
		return p_39746_.canEquip(equipmentslot, owner);
	}

	/**
	 * Return whether this slot's stack can be taken from this slot.
	 */
	public boolean mayPickup(Player p_39744_) {
		ItemStack itemstack = this.getItem();
		return !itemstack.isEmpty() && !p_39744_.isCreative() && EnchantmentHelper.hasBindingCurse(itemstack) ? false
				: super.mayPickup(p_39744_);
	}

	public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
		return Pair.of(InventoryMenu.BLOCK_ATLAS, TEXTURE_EMPTY_SLOTS[equipmentslot.getIndex()]);
	}

}
