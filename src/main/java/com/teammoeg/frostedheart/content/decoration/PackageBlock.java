package com.teammoeg.frostedheart.content.decoration;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.base.block.FHBaseBlock;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class PackageBlock extends FHBaseBlock {
    private static Integer typeCount = 4;
    private static IntegerProperty TYPE = IntegerProperty.create("packagetype", 0, typeCount - 1);
    private static Integer colorCount = 3;
    private static IntegerProperty COLOR = IntegerProperty.create("packagecolor", 0, colorCount - 1);
    static final VoxelShape shape = Block.box(0, 0, 0, 16, 15, 16);
    public PackageBlock(AbstractBlock.Properties blockProps) {
        super(blockProps);
        this.registerDefaultState(this.stateDefinition.any().setValue(TYPE, 0).setValue(COLOR, 0));
    }

    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
        builder.add(COLOR);
    }

    public void setPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Integer finalType = Math.abs(RANDOM.nextInt()) % typeCount;
        Integer finalColor = Math.abs(RANDOM.nextInt()) % colorCount;
        BlockState newState = this.stateDefinition.any().setValue(TYPE, finalType).setValue(COLOR, finalColor);
        worldIn.setBlockAndUpdate(pos, newState);
    }

    @Override
    public void playerWillDestroy(World worldIn, BlockPos pos, BlockState state, PlayerEntity player) {
        super.playerWillDestroy(worldIn, pos, state, player);
        int count = Math.abs(RANDOM.nextInt()) % 5 + 1;
        popResource(worldIn, pos, new ItemStack(Items.POTATO, count));
    }

    @Override
    public void wasExploded(World worldIn, BlockPos pos, Explosion explosionIn) {
        super.wasExploded(worldIn, pos, explosionIn);
        int count = Math.abs(RANDOM.nextInt()) % 2 + 1;
        popResource(worldIn, pos, new ItemStack(Items.POTATO, count));
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
