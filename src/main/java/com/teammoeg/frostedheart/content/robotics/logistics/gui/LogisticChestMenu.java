package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.client.cui.menu.CUISlotItemHandler;
import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.frostedheart.content.robotics.logistics.workers.LogisticStatusBlockEntity;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class LogisticChestMenu<T extends CBlockEntity&LogisticStatusBlockEntity> extends CBlockEntityMenu<T> {
	final CDataSlot<Integer> uplinkStatus=CCustomMenuSlot.SLOT_INT.create(this);
	final CDataSlot<Integer> networkStatus=CCustomMenuSlot.SLOT_INT.create(this);
	public LogisticChestMenu(MenuType<?> pMenuType,T blockEntity, int pContainerId, Inventory player,IItemHandler handler) {
		super(pMenuType, blockEntity, pContainerId, player.player, 27);
		
        for (int j = 0; j < 3; ++j) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new CUISlotItemHandler(handler, k + j*9 , 8 + k * 18, 28 + j * 18));
            }
        }
        uplinkStatus.bind(blockEntity::getUplinkStatus);
        networkStatus.bind(blockEntity::getNetworkStatus);
        
	}
	public LogisticChestMenu(MenuType<?> pMenuType,T blockEntity, int pContainerId, Inventory player) {
		super(pMenuType, blockEntity, pContainerId, player.player, 27);
		ItemStackHandler handler=new ItemStackHandler(27);
        for (int j = 0; j < 3; ++j) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new CUISlotItemHandler(handler, k + j*9 , 8 + k * 18, 28 + j * 18));
            }
        }
	}
	
}
