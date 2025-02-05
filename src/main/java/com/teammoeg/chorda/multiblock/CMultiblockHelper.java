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

package com.teammoeg.chorda.multiblock;

import java.util.Optional;
import java.util.function.Supplier;

import com.teammoeg.chorda.util.CUtils;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockBEHelper;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockLevel;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockBE;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.common.blocks.multiblocks.blockimpl.MultiblockContext;
import blusunrize.immersiveengineering.common.blocks.multiblocks.blockimpl.WrappingMultiblockContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CMultiblockHelper {
	private static final Supplier<RuntimeException> noMultiblockExists=()->new IllegalStateException("Multiblock is not valid");
	private CMultiblockHelper() {
	}
	public static Optional<IMultiblockBEHelper<?>> getBEHelper(Level level,BlockPos pos){
		if(CUtils.getExistingTileEntity(level, pos) instanceof IMultiblockBE te) {
			return Optional.of(te.getHelper());
		}
		return Optional.empty();
	}
	public static Optional<IMultiblockLogic<?>> getMultiblockLogic(Level level,BlockPos pos){
		return getBEHelper(level,pos).map(t->t.getMultiblock().logic());
	}
	public static Optional<IMultiblockBEHelper<?>> getBEHelper(IMultiblockLevel level) {
		if(level.getBlockEntity(BlockPos.ZERO) instanceof IMultiblockBE te) 
			return Optional.of(te.getHelper());
		return Optional.empty();
	}
	public static Optional<MultiblockRegistration<?>> getMultiblockOptional(IMultiblockContext<?> ctx){
		unwrap(ctx);
		if(ctx instanceof MultiblockContext mbctx) {
			return Optional.of(mbctx.multiblock());
		}
		return getBEHelper(ctx.getLevel()).map(t->t.getMultiblock());
	}
	
	public static MultiblockRegistration<?> getMultiblock(IMultiblockContext<?> ctx){
		return getMultiblockOptional(ctx).orElseThrow(noMultiblockExists);
	}
	public static Optional<IMultiblockLogic<?>> getMultiblockLogic(IMultiblockContext<?> ctx){
		return getMultiblockOptional(ctx).map(t->t.logic());
	}
	public static Vec3i getSize(IMultiblockContext<?> ctx) {
		return getMultiblock(ctx).size(ctx.getLevel().getRawLevel());
	}
	public static BlockPos getMasterPos(IMultiblockContext<?> ctx) {
		return getMultiblock(ctx).masterPosInMB();
	}
	public static BlockPos getAbsoluteMaster(IMultiblockContext<?> ctx) {
		return ctx.getLevel().toAbsolute(getMasterPos(ctx));
	}
	public static BlockEntity getMasterBlock(IMultiblockContext<?> ctx) {
		return ctx.getLevel().getBlockEntity(getMasterPos(ctx));
	}
	private static IMultiblockContext<?> unwrap(IMultiblockContext<?> ctx){
		while(ctx instanceof WrappingMultiblockContext wctx) {
			ctx=wctx.inner();
		}
		return ctx;
	}
}
