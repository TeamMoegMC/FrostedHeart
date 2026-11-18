package com.teammoeg.frostedheart.content.agriculture.tree;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/** Independent cultivation sapling; deliberately does not inherit vanilla bone-meal/tree event behavior. */
public final class CultivatedOakBlock extends BushBlock implements EntityBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 4);
    public CultivatedOakBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AGE); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CultivatedOakBlockEntity(pos, state); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int age = state.getValue(AGE);
        return Block.box(5 - age * .5, 0, 5 - age * .5, 11 + age * .5, 6 + age * 2.5, 11 + age * .5);
    }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof CultivatedOakBlockEntity sapling)
            sapling.advance(level, WorldTemperature.block(level, pos));
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CultivatedOakBlockEntity sapling)
            player.displayClientMessage(sapling.status(), false);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
