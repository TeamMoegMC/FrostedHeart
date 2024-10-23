package com.teammoeg.frostedheart.base.menu;

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

public class MultiblockContainer<S extends IMultiblockState, C extends AbstractContainerMenu>
{
	private final RegistryObject<MenuType<C>> type;
	private final MultiBlockMenuConstructor<S, C> factory;

	public MultiblockContainer(RegistryObject<MenuType<C>> type, MultiBlockMenuConstructor<S, C> factory) {
		super();
		this.type = type;
		this.factory = factory;
	}



	public MenuProvider provide(IMultiblockContext<S> ctx, BlockPos relativeClicked)
	{
		return new MenuProvider(){

			@Override
			public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
				return factory.construct(getType(), pContainerId, pPlayerInventory, new IEContainerMenu.MultiblockMenuContext<S>(ctx, ctx.getLevel().toAbsolute(relativeClicked)));
			}

			@Override
			public Component getDisplayName() {
				return Component.empty();
			}
			
		};
	}

	public IMultiblockComponent<S> createComponent(){
		return new FHMenuComponent<>(this);
	}



	public MenuType<C> getType()
	{
		return type.get();
	}
}