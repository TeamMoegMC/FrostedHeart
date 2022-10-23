/*
 * Copyright (c) 2022 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.incubator;

import com.teammoeg.frostedheart.base.block.FHGuiBlock;
import com.teammoeg.frostedheart.base.item.FHBlockItem;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.RegistryObject;
import javax.annotation.Nullable;

public class IncubatorBlock extends FHGuiBlock /*implements ILiquidContainer */{
    static DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    static BooleanProperty LIT = BlockStateProperties.LIT;
    private RegistryObject<TileEntityType<?>> type;

    public IncubatorBlock(String name, Properties p, RegistryObject<TileEntityType<?>> type) {
        super(name, p, FHBlockItem::new);
        this.type = type;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING,LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState().with(HORIZONTAL_FACING, context.getPlacementHorizontalFacing()).with(LIT,false);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return type.get().create();
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }
   /* @Override
    public boolean canContainFluid(IBlockReader w, BlockPos p, BlockState s, Fluid f) {
        TileEntity te = w.getTileEntity(p);
        if (te instanceof IncubatorTileEntity) {
        	IncubatorTileEntity ele = (IncubatorTileEntity) te;
            if (ele.fluid[0].fill(new FluidStack(f, 1000), IFluidHandler.FluidAction.SIMULATE) == 1000)
                return true;
        }
        return false;
    }

    @Override
    public boolean receiveFluid(IWorld w, BlockPos p, BlockState s,
                                FluidState f) {
        TileEntity te = w.getTileEntity(p);
        if (te instanceof IncubatorTileEntity) {
        	IncubatorTileEntity ele = (IncubatorTileEntity) te;
            if (ele.fluid[0].fill(new FluidStack(f.getFluid(), 1000), IFluidHandler.FluidAction.SIMULATE) == 1000) {
                ele.fluid[0].fill(new FluidStack(f.getFluid(), 1000), IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
        }
        return false;
    }*/

	@Override
	public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player,
			Hand hand, BlockRayTraceResult hit) {

		if (FluidUtil.interactWithFluidHandler(player, hand,world, pos,hit.getFace()))
			return ActionResultType.SUCCESS;
		return super.onBlockActivated(state, world, pos, player, hand, hit);
	}


}
