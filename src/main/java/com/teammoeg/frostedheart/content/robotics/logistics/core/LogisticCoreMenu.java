package com.teammoeg.frostedheart.content.robotics.logistics.core;

import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.chorda.menu.CMultiblockMenu;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.robotics.MachineTypes;
import com.teammoeg.frostedheart.content.robotics.labor.LaborSlot;
import com.teammoeg.frostedheart.content.robotics.labor.LaborType;
import com.teammoeg.frostedheart.content.robotics.labor.MachineLevelManager;

import blusunrize.immersiveengineering.common.gui.IEContainerMenu.MultiblockMenuContext;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class LogisticCoreMenu extends CMultiblockMenu<LogisticState>{


	public LaborSlot laborSlot = new LaborSlot(this,MachineTypes.LOGISTIC);
	public CDataSlot<Integer> taskSize = CCustomMenuSlot.SLOT_INT.create(this);
	public CDataSlot<Integer> workerSize = CCustomMenuSlot.SLOT_INT.create(this);
	public CDataSlot<Integer> networkSize = CCustomMenuSlot.SLOT_INT.create(this);
	public LogisticCoreMenu(MenuType<?> pMenuType, int pContainerId, Inventory inventoryPlayer) {
		super(pMenuType, pContainerId, inventoryPlayer.player, 0);
	}

	public LogisticCoreMenu(MenuType<?> pMenuType, int pContainerId, Inventory inventoryPlayer, MultiblockMenuContext<LogisticState> ctx) {
		super(pMenuType, pContainerId, inventoryPlayer.player, ctx, 0);
		LogisticState state=ctx.mbContext().getState();
		state.getTeamData().ifPresent(t->{
			MachineLevelManager data=t.getData(FHSpecialDataTypes.LABOUR_DATA);
			laborSlot.bind(data, state.pos);
		});
		taskSize.bind(state.ln::queueLength);
		workerSize.bind(state.ln::workerLength);
		networkSize.bind(state.ln::networkSize);
	}

	@Override
	public void receiveMessage(short btnId, int state) {
		super.receiveMessage(btnId, state);
		var mbState=super.getMenuContext().mbContext().getState();
		mbState.getTeamData().ifPresent(t->{
			MachineLevelManager data=t.getData(FHSpecialDataTypes.LABOUR_DATA);
			data.setDesiredLevel(mbState.pos,LaborType.registry.byId(btnId),state);
			
		});
	}

}
