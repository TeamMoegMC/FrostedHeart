package com.teammoeg.frostedheart.content.climate.block;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class WardrobeMenu extends ClothesInventoryMenu {
	WardrobeBlockEntity blockEntity;
	public WardrobeMenu(int id, Inventory inventoryPlayer, WardrobeBlockEntity tile) {
		super(id, inventoryPlayer,37);
		blockEntity=tile;
		super.addPlayerInventory(inventoryPlayer, 8, 120, 178);
	}
	@Override
	public void removed(Player pPlayer) {
		super.removed(pPlayer);
		if (pPlayer instanceof ServerPlayer) {
			WardrobeBlock.setOpened(blockEntity.getLevel(),blockEntity.getBlockPos(),blockEntity.getBlockState(),pPlayer,false);
		}
	}
}
