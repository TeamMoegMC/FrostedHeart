package com.teammoeg.frostedheart.content.steamenergy.creative;

import com.simibubi.create.foundation.block.IBE;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class CreativeHeaterBlock extends HeatBlock implements IBE<CreativeHeaterBlockEntity> {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public CreativeHeaterBlock(Properties blockProps) {
        super(blockProps);
    }

    @Override
    public Class<CreativeHeaterBlockEntity> getBlockEntityClass() {
        return CreativeHeaterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CreativeHeaterBlockEntity> getBlockEntityType() {
        return FHBlockEntityTypes.CREATIVE_HEATER.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction nearestLookingDirection = context.getNearestLookingDirection();
        return defaultBlockState().setValue(FACING, context.getPlayer() != null && context.getPlayer()
                .isShiftKeyDown() ? nearestLookingDirection : nearestLookingDirection.getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }
}
