package com.teammoeg.frostedheart.content.adventure.block;

import com.cannolicatfish.rankine.blocks.RankineOreBlock;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class bloodBlock extends FHBaseBlock {
    private static IntegerProperty BLDT = IntegerProperty.create("bloodtype", 0, 3);
    private static IntegerProperty BLDC = IntegerProperty.create("bloodcolor", 0, 1);
    public bloodBlock(Properties blockProps) {
        super(blockProps);
        this.setDefaultState(this.stateContainer.getBaseState().with(BLDT, 0).with(BLDC, 0));
    }

    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(BLDC);
        builder.add(BLDT);
    }

    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Integer finalType = Math.abs(RANDOM.nextInt()) % 4;
        Integer finalColor = Math.abs(RANDOM.nextInt()) % 2;
        BlockState newState = this.stateContainer.getBaseState().with(BLDT, finalType).with(BLDC, finalColor);
        worldIn.setBlockState(pos, newState);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return Block.makeCuboidShape(0, 0, 0, 16, 1, 16);
    }
}


