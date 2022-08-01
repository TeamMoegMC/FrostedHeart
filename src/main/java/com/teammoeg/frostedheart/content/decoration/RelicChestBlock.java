package com.teammoeg.frostedheart.content.decoration;

import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.base.item.FHBlockItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

public class RelicChestBlock extends FHBaseBlock {
    protected static final VoxelShape X_SHAPE = Block.makeCuboidShape(0D, 0D, 2D, 16D, 13D, 14D);
    protected static final VoxelShape Z_SHAPE = Block.makeCuboidShape(2D, 0D, 0D, 14D, 13D, 16D);
    public static final EnumProperty<Direction.Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public RelicChestBlock(String name) {
        super(name, Block.Properties.create(Material.IRON).sound(SoundType.STONE).setRequiresTool()
                .harvestTool(ToolType.PICKAXE).hardnessAndResistance(35, 600).notSolid(), FHBlockItem::new);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        if (state.get(HORIZONTAL_AXIS) == Direction.Axis.X)
            return X_SHAPE;
        return Z_SHAPE;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState().with(HORIZONTAL_AXIS, context.getPlacementHorizontalFacing().rotateY().getAxis());
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return FHTileTypes.RELIC_CHEST.get().create();
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isRemote) {
            RelicChestTileEntity tile = (RelicChestTileEntity) worldIn.getTileEntity(pos);
            if (tile != null) {
                NetworkHooks.openGui((ServerPlayerEntity) player, tile, tile.getPos());
                worldIn.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.3F, 1.5F);
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.matchesBlock(newState.getBlock())) {
            TileEntity tileentity = worldIn.getTileEntity(pos);
            if (tileentity instanceof IIEInventory) {
                InventoryHelper.dropInventoryItems(worldIn, pos, (IInventory) tileentity);
            }

            super.onReplaced(state, worldIn, pos, newState, isMoving);
        }
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        switch (rot) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                switch (state.get(HORIZONTAL_AXIS)) {
                    case Z:
                        return state.with(HORIZONTAL_AXIS, Direction.Axis.X);
                    case X:
                        return state.with(HORIZONTAL_AXIS, Direction.Axis.Z);
                    default:
                        return state;
                }
            default:
                return state;
        }
    }
}
