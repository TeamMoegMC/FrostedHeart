package com.teammoeg.frostedheart.content.robotics.logistics.core;

import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.chorda.menu.CMultiblockMenu;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.robotics.MachineLevelManager;

import blusunrize.immersiveengineering.common.gui.IEContainerMenu.MultiblockMenuContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

public class LogisticCoreMenu extends CMultiblockMenu<LogisticState>{

	public CDataSlot<Integer> currentLevel = CCustomMenuSlot.SLOT_INT.create(this);
	public CDataSlot<Integer> setLevel = CCustomMenuSlot.SLOT_INT.create(this);
	public LogisticCoreMenu(MenuType<?> pMenuType, int pContainerId, Player player) {
		super(pMenuType, pContainerId, player, 0);
	}

	public LogisticCoreMenu(MenuType<?> pMenuType, int pContainerId, Player player, MultiblockMenuContext<LogisticState> ctx) {
		super(pMenuType, pContainerId, player, ctx, 0);
		LogisticState state=ctx.mbContext().getState();
		state.getTeamData().ifPresent(t->{
			MachineLevelManager data=t.getData(FHSpecialDataTypes.LABOUR_DATA);
			setLevel.bind(()->data.getMachine(state.pos).getDesiredLevel());
			currentLevel.bind(()->data.getMachine(state.pos).getActualLevel());
		});
	}

}
