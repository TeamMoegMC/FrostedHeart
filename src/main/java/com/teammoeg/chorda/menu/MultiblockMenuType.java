package com.teammoeg.chorda.menu;

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
				return Component.empty();
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