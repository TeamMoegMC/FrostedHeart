package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.chorda.menu.CBaseMenu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class LogisticChestMenu extends CBaseMenu {

	public LogisticChestMenu(MenuType<?> pMenuType, int pContainerId, Inventory player,IItemHandler handler) {
		super(pMenuType, pContainerId, player.player, 27);
		
        for (int j = 0; j < 3; ++j) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new SlotItemHandler(handler, k + j*9 , 8 + k * 18, 28 + j * 18));
            }
        }
        super.addPlayerInventory(player, 8, 118, 176);
	}
	public LogisticChestMenu(MenuType<?> pMenuType, int pContainerId, Inventory player) {
		this(pMenuType, pContainerId, player, new ItemStackHandler(27));
	}
}
