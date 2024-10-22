package com.teammoeg.frostedheart;

import java.util.function.BiFunction;

import blusunrize.immersiveengineering.common.gui.BlockEntityInventory;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class FHBlockEntityContainer<T extends BlockEntity> extends FHBaseContianer {
	protected T blockEntity;
	public Container inv;

	public T getBlock() {
		return blockEntity;
	}

	protected FHBlockEntityContainer(MenuType<?> pMenuType, T blockEntity, int pContainerId, int inv_start) {
		super(pMenuType, pContainerId, inv_start);
		if (blockEntity instanceof IIEInventory)
			inv = new BlockEntityInventory(blockEntity, this);
		else if (blockEntity instanceof Container cont)
			inv = cont;
		this.blockEntity = blockEntity;

	}

	@Override
	public boolean stillValid(Player pPlayer) {
		return !blockEntity.isRemoved();
	}
}