package com.teammoeg.frostedheart.steamenergy;

import java.util.function.BiFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.block.FHGuiBlock;
import com.teammoeg.frostedheart.content.FHTileTypes;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class RadiatorBlock extends FHGuiBlock  implements ISteamEnergyBlock{
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public RadiatorBlock(String name, Properties blockProps,
			BiFunction<Block, net.minecraft.item.Item.Properties, Item> createItemBlock) {
		super(name, blockProps, createItemBlock);
	}


    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.RADIATOR.get().create();
    }
    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(LIT);
    }
    @Override
	public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
			boolean isMoving) {
		TileEntity te=Utils.getExistingTileEntity(worldIn,pos);
		if(te instanceof RadiatorTileEntity) {
			Vector3i vec=fromPos.subtract(pos);
			Direction dir=Direction.getFacingFromVector(vec.getX(),vec.getY(),vec.getZ());
			((IConnectable) te).connectAt(dir);
		}
	}


	@Override
	public boolean canConnectFrom(IBlockDisplayReader world, BlockPos pos, BlockState state, Direction dir) {
		return dir!=Direction.UP;
	}

}
