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

package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.List;
import java.util.function.Supplier;

import com.teammoeg.chorda.block.CGuiBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public class LogisticChestBlock<T extends BlockEntity> extends CGuiBlock<T> {
	Supplier<BlockEntityType<T>> blockEntity;
	VoxelShape shape=Block.box(0, 0, 0, 16, 12, 16);
	Component description;

	public LogisticChestBlock(Properties blockProps, Supplier<BlockEntityType<T>> blockEntity, Component description) {
		super(blockProps);
		this.blockEntity = blockEntity;
		this.description = description;
	}


	@Override
	public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
		return 1f;
	}

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
    	return shape;
    }

	@Override
	public Supplier<BlockEntityType<T>> getBlock() {
		return blockEntity;
	}

	@Override
	public void onRemove(BlockState state,Level level,BlockPos pos,BlockState newState,boolean isMoving) {
		if(state.getBlock()!=newState.getBlock()&&!level.isClientSide) {
			BlockEntity blockEntity=level.getBlockEntity(pos);
			if(blockEntity!=null)
				blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler->{
					for(int slot=0;slot<handler.getSlots();slot++) {
						int count=handler.getStackInSlot(slot).getCount();
						if(count>0)
							Containers.dropItemStack(level,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,handler.extractItem(slot,count,false));
					}
				});
		}
		super.onRemove(state,level,pos,newState,isMoving);
	}

	@Override
	public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		if (description != null)
			tooltip.add(description);
	}

}
