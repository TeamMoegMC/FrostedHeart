/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft;

import net.minecraft.core.BlockPos;
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
