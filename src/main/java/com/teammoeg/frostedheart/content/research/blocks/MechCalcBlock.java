/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research.blocks;

import java.util.List;
import java.util.function.Supplier;

import com.simibubi.create.foundation.utility.VoxelShaper;
import com.teammoeg.chorda.block.FHEntityBlock;
import com.teammoeg.chorda.block.FHKineticBlock;
import com.teammoeg.chorda.creativeTab.CreativeTabItemHelper;
import com.teammoeg.chorda.creativeTab.ICreativeModeTabItem;
import com.teammoeg.chorda.util.lang.Components;
import com.teammoeg.frostedheart.bootstrap.client.FHTabs;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;

public class MechCalcBlock extends FHKineticBlock implements FHEntityBlock<MechCalcTileEntity>,ICreativeModeTabItem{
    static final VoxelShaper shape = VoxelShaper.forDirectional(Shapes.or(Block.box(0, 0, 0, 16, 9, 16), Block.box(0, 9, 0, 16, 16, 13)), Direction.SOUTH);

    public MechCalcBlock( Properties blockProps) {
        super(blockProps);
        this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
    }


    @Override
    public void appendHoverText(ItemStack stack, BlockGetter worldIn, List<Component> tooltip,
                               TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        if (stack.hasTag() && stack.getTag().getBoolean("prod")) {
            tooltip.add(Components.str("For Display Only"));
        }
    }





    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise().getAxis();
    }


    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return shape.get(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }


    @Override
    public boolean hasShaftTowards(LevelReader arg0, BlockPos arg1, BlockState state, Direction dir) {
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise().getAxis() == dir.getAxis();
    }




    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult superResult = super.use(state, world, pos, player, hand, hit);
        if (superResult.consumesAction() || player instanceof FakePlayer)
            return superResult;
        BlockEntity te = Utils.getExistingTileEntity(world, pos);
        if (te instanceof MechCalcTileEntity)
            return ((MechCalcTileEntity) te).onClick(player);
        return superResult;
    }


    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (stack.hasTag() && stack.getTag().getBoolean("prod")) {
            BlockEntity te = Utils.getExistingTileEntity(worldIn, pos);
            if (te instanceof MechCalcTileEntity) {
                ((MechCalcTileEntity) te).doProduct = false;
            }
        }

        super.setPlacedBy(worldIn, pos, state, placer, stack);

    }




	@Override
	public void fillItemCategory(CreativeTabItemHelper helper) {
        if(helper.isType(FHTabs.itemGroup)) {
            helper.accept(this);
            ItemStack is = new ItemStack(this);
            is.getOrCreateTag().putBoolean("prod", true);
            helper.accept(is);
        }
	}


	@Override
	public Supplier<BlockEntityType<MechCalcTileEntity>> getBlock() {
		return FHBlockEntityTypes.MECH_CALC;
	}


	/*@Override
	public SpeedLevel getMinimumRequiredSpeedLevel() {
		return SpeedLevel.of(16);
	}*/


}
