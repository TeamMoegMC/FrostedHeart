package com.teammoeg.frostedheart.steamenergy;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.block.FHBaseBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiFunction;

public class DebugHeaterBlock extends FHBaseBlock implements ISteamEnergyBlock {
    public DebugHeaterBlock(String name, Properties blockProps,
                            BiFunction<Block, net.minecraft.item.Item.Properties, Item> createItemBlock) {
        super(name, blockProps, createItemBlock);
    }


    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHContent.FHTileTypes.DEBUGHEATER.get().create();
    }

    /*@Override
    public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onBlockAdded(state, worldIn, pos, oldState, isMoving);
        for(Direction d:Direction.values()) {
            TileEntity te=Utils.getExistingTileEntity(worldIn,pos.offset(d));
            if(te instanceof HeatPipeTileEntity)
                ((HeatPipeTileEntity) te).connectAt(d.getOpposite());
        }
    }


    @Override
    public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onReplaced(state, worldIn, pos, newState, isMoving);
        for(Direction d:Direction.values()) {
            TileEntity te=Utils.getExistingTileEntity(worldIn,pos.offset(d));
            if(te instanceof HeatPipeTileEntity)
                ((HeatPipeTileEntity) te).disconnectAt(d.getOpposite());
        }
    }*/
    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
                                boolean isMoving) {
        TileEntity te = Utils.getExistingTileEntity(worldIn, fromPos);
        if (te instanceof IConnectable) {
            Vector3i vec = pos.subtract(fromPos);
            Direction dir = Direction.getFacingFromVector(vec.getX(), vec.getY(), vec.getZ());
            ((IConnectable) te).connectAt(dir);
        }
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }


    @Override
    public boolean canConnectFrom(IBlockDisplayReader world, BlockPos pos, BlockState state, Direction dir) {
        return true;
    }
}
