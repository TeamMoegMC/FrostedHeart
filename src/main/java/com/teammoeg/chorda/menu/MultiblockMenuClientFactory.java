package com.teammoeg.chorda.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

@FunctionalInterface
public interface MultiblockMenuClientFactory<C extends AbstractContainerMenu>
{
	C create(MenuType<C> type, int windowId, Inventory inventoryPlayer);
}
