package com.teammoeg.frostedheart.content.decoration;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.base.block.FHBaseBlock;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

public class PebbleBlock extends FHBaseBlock {
    private static Integer typeCount = 7;
    private static IntegerProperty TYPE = IntegerProperty.create("pebbletype", 0, typeCount - 1);
    private static Integer colorCount = 3;
    private static IntegerProperty COLOR = IntegerProperty.create("pebblecolor", 0, colorCount - 1);
    static final VoxelShape shape = Block.box(0, 0, 0, 16, 9, 16);
    static final VoxelShape shape2 = Block.box(0, 0, 0, 16, 7, 16);
    static final VoxelShape shape3 = Block.box(0, 0, 0, 16, 5, 16);
    static final VoxelShape shape4 = Block.box(0, 0, 0, 16, 2, 16);
    public PebbleBlock(BlockBehaviour.Properties blockProps) {
        super( blockProps);
        this.registerDefaultState(this.stateDefinition.any().setValue(TYPE, 0).setValue(COLOR, 0));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
        builder.add(COLOR);
    }

    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Integer finalType = Math.abs(RANDOM.nextInt()) % typeCount;
        Integer finalColor = Math.abs(RANDOM.nextInt()) % colorCount;
        BlockState newState = this.stateDefinition.any().setValue(TYPE, finalType).setValue(COLOR, finalColor);
        worldIn.setBlockAndUpdate(pos, newState);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos,
                                        CollisionContext context) {
        if(state.getValue(TYPE) <= 1)return shape;
        if(state.getValue(TYPE) <= 2)return shape2;
        if(state.getValue(TYPE) <= 4)return shape3;
        return shape4;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        if(state.getValue(TYPE) <= 1)return shape;
        if(state.getValue(TYPE) <= 2)return shape2;
        if(state.getValue(TYPE) <= 4)return shape3;
        return shape4;
    }
}
