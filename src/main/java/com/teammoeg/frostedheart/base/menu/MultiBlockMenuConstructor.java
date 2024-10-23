package com.teammoeg.frostedheart.base.menu;

import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.common.gui.IEContainerMenu.MultiblockMenuContext;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
@FunctionalInterface
public interface MultiBlockMenuConstructor<T extends IMultiblockState,C extends AbstractContainerMenu>
{
	C construct(MenuType<C> type, int windowId, Inventory inventoryPlayer, MultiblockMenuContext<T> te);
}