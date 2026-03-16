/*
 * Copyright (c) 2026 TeamMoeg
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

/**
 * 多方块结构菜单的基类。与 Immersive Engineering 的多方块系统集成，
 * 自动处理服务端和客户端的构造差异以及多方块有效性验证。
 * <p>
 * Base class for multiblock structure menus. Integrates with Immersive Engineering's
 * multiblock system, automatically handling server/client constructor differences
 * and multiblock validity validation.
 *
 * @param <R> 多方块状态类型 / the multiblock state type
 */
public class CMultiblockMenu<R extends IMultiblockState> extends CBaseMenu {
	@Getter
	private MultiblockMenuContext<R> menuContext;
	/**
	 * 服务端构造函数，包含完整的多方块上下文。
	 * <p>
	 * Server-side constructor with full multiblock context.
	 */
	public CMultiblockMenu(MenuType<?> pMenuType, int pContainerId, Player player, MultiblockMenuContext<R> ctx, int inv_start) {
		super(pMenuType, pContainerId, player, inv_start);
		this.menuContext=ctx;
	}
	/**
	 * 客户端构造函数，不含多方块上下文。
	 * <p>
	 * Client-side constructor without multiblock context.
	 */
	public CMultiblockMenu(MenuType<?> pMenuType, int pContainerId, Player player, int inv_start) {
		super(pMenuType, pContainerId, player, inv_start);
	}
	/**
	 * 客户端构造函数，带有点击位置以恢复多方块上下文。
	 * <p>
	 * Client-side constructor with clicked position to restore multiblock context.
	 */
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
