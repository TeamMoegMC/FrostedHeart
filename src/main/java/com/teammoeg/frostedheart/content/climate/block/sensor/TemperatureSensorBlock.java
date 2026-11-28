/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.block.sensor;

import java.util.function.Supplier;
import com.teammoeg.chorda.block.CGuiBlock;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class TemperatureSensorBlock extends CGuiBlock<TemperatureSensorBlockEntity> {
    public TemperatureSensorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.POWERED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.POWERED);
    }
    @Override public Supplier<BlockEntityType<TemperatureSensorBlockEntity>> getBlock() {
        return FHBlockEntityTypes.TEMPERATURE_SENSOR;
    }
    @Override public void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                   BlockState replacement, boolean moving) {
        super.onRemove(state, level, pos, replacement, moving);
        if (!state.is(replacement.getBlock()) && state.getValue(BlockStateProperties.POWERED)) {
            level.updateNeighborsAt(pos, this);
            for (Direction side : Direction.values()) level.updateNeighborsAt(pos.relative(side), this);
        }
    }
    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(BlockStateProperties.POWERED) ? 15 : 0;
    }
    @Override public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return getSignal(state, level, pos, side);
    }
}
