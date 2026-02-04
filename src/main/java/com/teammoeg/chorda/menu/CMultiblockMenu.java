/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
