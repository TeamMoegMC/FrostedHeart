package com.teammoeg.chorda.menu;

import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IMultiblockComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public record CMenuComponent<S extends IMultiblockState,C extends AbstractContainerMenu>(MultiblockContainer<S,C> cont) implements IMultiblockComponent<S> {
	

	@Override
	public InteractionResult click(IMultiblockContext<S> ctx, BlockPos posInMultiblock, Player player, InteractionHand hand, BlockHitResult absoluteHit, boolean isClient) {
		if(!isClient)
			NetworkHooks.openScreen((ServerPlayer) player, cont.provide(ctx, posInMultiblock));
		return InteractionResult.SUCCESS;
	}

}
