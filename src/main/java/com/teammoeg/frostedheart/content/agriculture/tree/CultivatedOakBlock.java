package com.teammoeg.frostedheart.content.agriculture.tree;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/** Independent cultivation sapling; deliberately does not inherit vanilla bone-meal/tree event behavior. */
public final class CultivatedOakBlock extends BushBlock implements EntityBlock, SimpleWaterloggedBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 4);
    private final ResourceLocation profileId;
    public CultivatedOakBlock(Properties properties) { this(properties, "oak"); }
    public CultivatedOakBlock(Properties properties, String species) {
        super(properties);
        profileId = new ResourceLocation(FHMain.MODID, species);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0)
                .setValue(BlockStateProperties.WATERLOGGED, false));
    }
    public ResourceLocation profileId() { return profileId; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AGE, BlockStateProperties.WATERLOGGED); }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(CultivatedSaplingPlacement.originalFor(this));
    }
    private boolean isMangrove() { return profileId.getPath().equals("mangrove"); }
    @Override protected boolean mayPlaceOn(BlockState soil, BlockGetter level, BlockPos pos) {
        return isMangrove() && soil.is(Blocks.CLAY) || super.mayPlaceOn(soil, level, pos);
    }
    @Override public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state,
            Fluid fluid) {
        return isMangrove() && SimpleWaterloggedBlock.super.canPlaceLiquid(level, pos, state, fluid);
    }
    @Override public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state,
            FluidState fluid) {
        return isMangrove() && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluid);
    }
    @Override public FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean wet = state.getValue(BlockStateProperties.WATERLOGGED);
        if (wet) level.scheduleTick(pos, Fluids.WATER,
                Fluids.WATER.getTickDelay(level));
        BlockState updated = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        return wet && updated.isAir() ? Blocks.WATER.defaultBlockState() : updated;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CultivatedOakBlockEntity(pos, state); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int age = state.getValue(AGE);
        return Block.box(5 - age * .5, 0, 5 - age * .5, 11 + age * .5, 6 + age * 2.5, 11 + age * .5);
    }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) return; // Same entry guard as Forge SaplingBlock.
        if (level.getBlockEntity(pos) instanceof CultivatedOakBlockEntity sapling)
            sapling.advance(level, WorldTemperature.getTempAtBlock(level, pos), random);
    }
}
