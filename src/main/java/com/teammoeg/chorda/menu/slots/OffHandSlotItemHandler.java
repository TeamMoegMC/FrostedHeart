package com.teammoeg.chorda.menu.slots;

import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class OffHandSlotItemHandler extends ArmorSlotItemHandler {



	public OffHandSlotItemHandler(Player owner, IItemHandler pContainer, int pSlot, int pX, int pY) {
		super(owner, EquipmentSlot.OFFHAND, pContainer, pSlot, pX, pY);
	}

	@Override
	public int getMaxStackSize() {
		return container.getMaxStackSize();
	}

	@Override
	public boolean mayPlace(ItemStack p_39746_) {
		return true;
	}

	@Override
	public boolean mayPickup(Player p_39744_) {
		return true;
	}

	@Override
	public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
		return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
	}

}
