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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Startup-only coarse classification. Ventilation is not occupied volume. */
public final class StateStaticThermalResolver {
    private StateStaticThermalResolver() {}
    /** Full material contact faces, in BlockFace's X/Y/Z paired order. */
    public static int fullContactFaces(BlockState state) {
        if (state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) return 63;
        if (state.getBlock().hasDynamicShape()) return ventilation(state) == 0 ? 63 : 0;
        try {
            VoxelShape shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            int faces = 0;
            if (Block.isFaceFull(shape, Direction.WEST)) faces |= 1;
            if (Block.isFaceFull(shape, Direction.EAST)) faces |= 2;
            if (Block.isFaceFull(shape, Direction.DOWN)) faces |= 4;
            if (Block.isFaceFull(shape, Direction.UP)) faces |= 8;
            if (Block.isFaceFull(shape, Direction.NORTH)) faces |= 16;
            if (Block.isFaceFull(shape, Direction.SOUTH)) faces |= 32;
            return faces;
        } catch (RuntimeException unsupported) {
            return 0;
        }
    }
    public static int ventilation(BlockState state) {
        if (!state.getFluidState().isEmpty()) return 0;
        Block block = state.getBlock();
        if (block instanceof DoorBlock || block instanceof TrapDoorBlock)
            return state.getValue(BlockStateProperties.OPEN) ? 100 : 0;
        // Registered generator multiblocks use a full collision cube even though
        // their renderer/model is dynamic.
        if (state.is(com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks.Registration.GENERATOR_T1.block().get())
                || state.is(com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks.Registration.GENERATOR_T2.block().get()))
            return 0;
        if (block.hasDynamicShape()) return 75;
        try {
            VoxelShape shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            if (shape.isEmpty()) return 100;
            return Block.isShapeFullBlock(shape) ? 0 : 75;
        } catch (RuntimeException unsupported) {
            return 75;
        }
    }
}
