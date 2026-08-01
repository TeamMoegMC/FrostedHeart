package com.teammoeg.frostedheart.content.decoration;

import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.utility.VoxelShaper;
import com.teammoeg.chorda.block.CBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FHScaffoldingStairsBlock extends CBlock implements SimpleWaterloggedBlock {

    protected static final VoxelShaper SHAPE = new AllShapes.Builder(Block.box(0, 14, 0, 16, 16, 4))
            .add(0, 6, 4, 16, 8, 12)
            .add(0, -2, 12, 16, 0, 16)
            .forHorizontal(Direction.NORTH);

    public FHScaffoldingStairsBlock(Properties blockProps) {
        super(blockProps);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.WATERLOGGED, false)
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(BlockStateProperties.WATERLOGGED, HorizontalDirectionalBlock.FACING);
        super.createBlockStateDefinition(pBuilder);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        var pos = pContext.getClickedPos();
        var level = pContext.getLevel();
        return defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER)
                .setValue(HorizontalDirectionalBlock.FACING, pContext.getHorizontalDirection());
    }

    public FluidState getFluidState(BlockState pState) {
        return pState.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
        if (pState.getValue(BlockStateProperties.WATERLOGGED)) {
            pLevel.scheduleTick(pPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
        }
        return super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return FHScaffoldingBlock.isHoldingScaffolding(pState, pLevel, pPos, pContext) ? Shapes.block() : SHAPE.get(pState.getValue(HorizontalDirectionalBlock.FACING));
    }
}
