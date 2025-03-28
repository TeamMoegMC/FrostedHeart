package com.teammoeg.chorda.menu;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.multiblock.CMultiblockHelper;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.common.gui.IEContainerMenu.MultiblockMenuContext;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

public class CMultiblockMenu<R extends IMultiblockState> extends CBaseMenu {
	@Getter
	private MultiblockMenuContext<R> menuContext;
	//Server constructor
	public CMultiblockMenu(MenuType<?> pMenuType, int pContainerId, Player player, MultiblockMenuContext<R> ctx, int inv_start) {
		super(pMenuType, pContainerId, player, inv_start);
		this.menuContext=ctx;
	}
	//Client constructor
	public CMultiblockMenu(MenuType<?> pMenuType, int pContainerId, Player player, int inv_start) {
		super(pMenuType, pContainerId, player, inv_start);
	}
	//Client constructor with clicked position
	public CMultiblockMenu(MenuType<?> pMenuType, int pContainerId, Player player, int inv_start,BlockPos pos) {
		super(pMenuType, pContainerId, player, inv_start);
		this.menuContext=new MultiblockMenuContext<R>((IMultiblockContext)CMultiblockHelper.getBEHelper(player.level(), pos).getContext(),pos);
	}
	@Override
	protected Validator buildValidator(Validator builder) {
		/*MultiblockRegistration<?> mb=CMultiblockHelper.getMultiblock(menuContext.mbContext());
		Vec3i otile = mb.size(menuContext.mbContext().getLevel().getRawLevel());
		BlockPos master = menuContext.mbContext().getLevel().toAbsolute(mb.masterPosInMB());*/
		if(menuContext==null)
			return super.buildValidator(builder);
		return super.buildValidator(builder).range(menuContext.clickedPos(), 8).custom(menuContext.mbContext().isValid());
	}
}
