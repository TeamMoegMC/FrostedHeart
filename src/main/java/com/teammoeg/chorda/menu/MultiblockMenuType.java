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

import com.teammoeg.chorda.lang.Components;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IMultiblockComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.common.gui.IEContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.RegistryObject;

public class MultiblockMenuType<S extends IMultiblockState, C extends AbstractContainerMenu>
{
	private final RegistryObject<MenuType<C>> type;
	private final MultiBlockMenuServerFactory<S, C> factory;

	public MultiblockMenuType(RegistryObject<MenuType<C>> type, MultiBlockMenuServerFactory<S, C> factory) {
		super();
		this.type = type;
		this.factory = factory;
	}



	public MenuProvider provide(IMultiblockContext<S> ctx, BlockPos relativeClicked)
	{
		return new MenuProvider(){

			@Override
			public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
				return factory.create(getType(), pContainerId, pPlayerInventory, new IEContainerMenu.MultiblockMenuContext<S>(ctx, ctx.getLevel().toAbsolute(relativeClicked)));
			}

			@Override
			public Component getDisplayName() {
				return Components.empty();
				//return CMultiblockHelper.getMultiblockOptional(ctx).map(t->t.block().get().getName()).orElse(Components.empty());
			}
			
		};
	}

	public IMultiblockComponent<S> createComponent(){
		return new CMultiblockMenuComponent<>(this);
	}



	public MenuType<C> getType()
	{
		return type.get();
	}
}