package com.teammoeg.frostedheart.content.decoration;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.base.block.FHBaseBlock;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class DeskBlock extends FHBaseBlock {
    private static Integer typeCount = 4;
    private static IntegerProperty TYPE = IntegerProperty.create("desktype", 0, typeCount - 1);
    static final VoxelShape shape = Block.box(0, 0, 0, 16, 16, 32);

    public DeskBlock(AbstractBlock.Properties blockProps) {
        super(blockProps);
        this.registerDefaultState(this.stateDefinition.any().setValue(TYPE, 0));
    }

    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    public void setPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Integer finalType = Math.abs(RANDOM.nextInt()) % typeCount;
        BlockState newState = this.stateDefinition.any().setValue(TYPE, finalType);
        worldIn.setBlockAndUpdate(pos, newState);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos,
                                        ISelectionContext context) {
        return shape;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return shape;
    }
}
