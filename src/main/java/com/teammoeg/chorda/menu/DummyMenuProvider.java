package com.teammoeg.chorda.menu;

import com.teammoeg.chorda.lang.Components;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;

public class DummyMenuProvider implements MenuProvider {
	MenuConstructor type;
	public DummyMenuProvider(MenuConstructor type) {
		this.type=type;
	}

	@Override
	public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
		return type.createMenu(pContainerId, pPlayerInventory, pPlayer);
	}

	@Override
	public Component getDisplayName() {
		return Components.str("");
	}

}
