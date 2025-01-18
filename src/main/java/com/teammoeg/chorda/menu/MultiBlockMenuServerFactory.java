package com.teammoeg.chorda.menu;

import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.common.gui.IEContainerMenu.MultiblockMenuContext;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
@FunctionalInterface
public interface MultiBlockMenuServerFactory<T extends IMultiblockState,C extends AbstractContainerMenu>
{
	C create(MenuType<C> type, int windowId, Inventory inventoryPlayer, MultiblockMenuContext<T> te);
}