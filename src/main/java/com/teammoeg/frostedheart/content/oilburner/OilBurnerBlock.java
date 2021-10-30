/*
 * Copyright (c) 2021 TeamMoeg
 *
 * This file is part of Steam Powered.
 *
 * Steam Powered is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Steam Powered is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Steam Powered. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.content.oilburner;

import java.util.List;
import java.util.function.BiFunction;

import com.simibubi.create.foundation.item.TooltipHelper;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.steampowered.content.boiler.BoilerTileEntity;
import com.simibubi.create.foundation.item.ItemDescription.Palette;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public class OilBurnerBlock extends FHBaseBlock  implements  ILiquidContainer{

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public OilBurnerBlock(String name, Properties blockProps,
                        BiFunction<Block, net.minecraft.item.Item.Properties, Item> createItemBlock) {
        super(name, blockProps, createItemBlock);
        this.setDefaultState(this.stateContainer.getBaseState().with(LIT, Boolean.FALSE));
    }
    @Override
    protected void fillStateContainer(Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(LIT);
    }

    @Override
    public TileEntity createTileEntity(BlockState state,IBlockReader world) {
        return FHContent.FHTileTypes.OIL_BURNER.get().create();
    }
    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public void onEntityWalk(World w, BlockPos p, Entity e) {
        if (w.getBlockState(p).get(LIT) == true)
            if (e instanceof LivingEntity)
                e.setFire(60);
    }
	@Override
	public boolean canContainFluid(IBlockReader w, BlockPos p, BlockState s, Fluid f) {
    	TileEntity te=w.getTileEntity(p);
    	if(te instanceof OilBurnerTileEntity) {
			OilBurnerTileEntity boiler=(OilBurnerTileEntity)te;
			if(boiler.input.fill(new FluidStack(f,1000),FluidAction.SIMULATE)==1000)
				return true;
    	}
    	return false;

	}
	@Override
	public boolean receiveFluid(IWorld w, BlockPos p, BlockState s, FluidState f) {
		TileEntity te=w.getTileEntity(p);
		if(te instanceof OilBurnerTileEntity) {
			OilBurnerTileEntity boiler=(OilBurnerTileEntity)te;
			if(boiler.input.fill(new FluidStack(f.getFluid(),1000),FluidAction.SIMULATE)==1000) {
				boiler.input.fill(new FluidStack(f.getFluid(),1000),FluidAction.EXECUTE);
				return true;
			}
		}
		return false;
	}
}
