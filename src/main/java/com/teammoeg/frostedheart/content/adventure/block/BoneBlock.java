package com.teammoeg.frostedheart.content.adventure.block;

import com.simibubi.create.foundation.utility.VoxelShaper;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

public class BoneBlock extends FHBaseBlock {
    private static IntegerProperty BNT = IntegerProperty.create("bonetype", 0, 5);
    static final VoxelShape shape = Block.makeCuboidShape(0, 0, 0, 16, 3, 16);
    static final VoxelShape shape2 = Block.makeCuboidShape(0, 0, 0, 16, 15, 16);
    public BoneBlock(String name, AbstractBlock.Properties blockProps, BiFunction<Block, Item.Properties, Item> createItemBlock) {
        super(name, blockProps, createItemBlock);
        this.setDefaultState(this.stateContainer.getBaseState().with(BNT, 0));
    }

    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(BNT);
    }

    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Integer finalType = Math.abs(RANDOM.nextInt()) % 5;
        BlockState newState = this.stateContainer.getBaseState().with(BNT, finalType);
        worldIn.setBlockState(pos, newState);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos,
                                        ISelectionContext context) {
        if (state.get(BNT) <= 0)
            return shape;
        return shape2;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        if (state.get(BNT) <= 0)
            return shape;
        return shape2;
    }
}
