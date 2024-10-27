package com.teammoeg.frostedheart.base.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

@FunctionalInterface
public interface ClientContainerConstructor<C extends AbstractContainerMenu>
{
	C construct(MenuType<C> type, int windowId, Inventory inventoryPlayer);
}
