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

package com.teammoeg.frostedheart.content.robotics.logistics.core;

import java.util.function.Function;

import com.teammoeg.chorda.multiblock.DisassembleListener;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.climate.block.generator.OwnedLogic;

import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IClientTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IServerTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockBEHelper;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.CapabilityPosition;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.ShapeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class LogisticCoreLogic implements IServerTickableComponent<LogisticState>, IClientTickableComponent<LogisticState>, IMultiblockLogic<LogisticState>, OwnedLogic<LogisticState>, DisassembleListener<LogisticState> {

	public LogisticCoreLogic() {
	}

	@Override
	public void onOwnerChange(IMultiblockContext<LogisticState> ctx) {
	}

	@Override
	public LogisticState createInitialState(IInitialMultiblockContext<LogisticState> capabilitySource) {
		return new LogisticState();
	}

	@Override
	public Function<BlockPos, VoxelShape> shapeGetter(ShapeType forType) {
		return a->Shapes.block();
	}

	@Override
	public void tickClient(IMultiblockContext<LogisticState> context) {
	}

	@Override
	public <T> LazyOptional<T> getCapability(IMultiblockContext<LogisticState> ctx, CapabilityPosition position, Capability<T> cap) {
		LogisticState state=ctx.getState();
		if(state.cap!=null&&cap==FHCapabilities.LOGISTIC.capability()) {
			return ctx.getState().cap.cast(ctx);
		}
		return LazyOptional.empty();
	
	}

	@Override
	public InteractionResult click(IMultiblockContext<LogisticState> ctx, BlockPos posInMultiblock, Player player, InteractionHand hand, BlockHitResult absoluteHit, boolean isClient) {
		return IServerTickableComponent.super.click(ctx, posInMultiblock, player, hand, absoluteHit, isClient);
	}

	@Override
	public void tickServer(IMultiblockContext<LogisticState> context) {
		LogisticState state=context.getState();
		state.initialize(context.getLevel().getRawLevel(),context.getLevel().getAbsoluteOrigin(),context::markMasterDirty);
		state.ticker.tick();
		state.ln.tick();
		
	}

	@Override
	public void onDisassemble(IMultiblock block,IMultiblockBEHelper<LogisticState> helper) {
		LogisticState state=helper.getState();
		if(state!=null)
			state.shutdown();
	}


}
