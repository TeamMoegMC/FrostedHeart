package com.teammoeg.frostedheart.common.block.cropblock;

import com.teammoeg.frostedheart.FHBlocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Random;
import java.util.function.BiFunction;

public class LeekBlock extends FHCropBlock {
    public static final IntegerProperty Leek_AGE = BlockStateProperties.AGE_0_5;
    private static final VoxelShape[] SHAPE = new VoxelShape[]{Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D), Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D), Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D)
            , Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 9.0D, 16.0D), Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D)};

    public LeekBlock(String name, AbstractBlock.Properties properties, BiFunction<Block, Item.Properties, Item> createItemBlock) {
        super(name, properties, createItemBlock);
    }

    public IntegerProperty getAgeProperty() {
        return Leek_AGE;
    }

    public int getMaxAge() {
        return 5;
    }

    protected IItemProvider getSeedsItem() {
        return FHBlocks.leek_block.asItem();
    }


    public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
        if (random.nextInt(2) != 0) {
            super.randomTick(state, worldIn, pos, random);
        }

    }

    protected int getBonemealAgeIncrease(World worldIn) {
        return super.getBonemealAgeIncrease(worldIn) / 5;
    }

    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(Leek_AGE);
    }

    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return SHAPE[state.get(this.getAgeProperty())];
    }
}


