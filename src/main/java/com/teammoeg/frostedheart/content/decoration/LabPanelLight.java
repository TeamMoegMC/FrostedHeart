/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.decoration;

import com.teammoeg.chorda.block.CBlock;
import com.teammoeg.chorda.creativeTab.CreativeTabItemHelper;
import com.teammoeg.chorda.creativeTab.ICreativeModeTabItem;
import com.teammoeg.frostedheart.bootstrap.client.FHTabs;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class LabPanelLight extends CBlock implements ICreativeModeTabItem {
//    public static final BooleanProperty ALWAYS_LIT = BooleanProperty.create("always_lit");
    public LabPanelLight(Properties blockProps) {
        super(blockProps);
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.FACING);
        builder.add(BlockStateProperties.LIT);
//        builder.add(ALWAYS_LIT);
    }


    @Override
    public BlockState rotate(BlockState pState, Rotation pRot) {
        return pState.setValue(BlockStateProperties.FACING, pRot.rotate(pState.getValue(BlockStateProperties.FACING)));
    }

    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.setValue(BlockStateProperties.FACING, pMirror.mirror(pState.getValue(BlockStateProperties.FACING)));
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(BlockStateProperties.FACING, pContext.getNearestLookingDirection().getOpposite())
                .setValue(BlockStateProperties.LIT,false);
    }
    @Override
    public void fillItemCategory(CreativeTabItemHelper helper) {
        if (helper.isType(FHTabs.building_blocks)) {
            helper.accept(this);
            ItemStack stack = new ItemStack(this);
            CompoundTag blockStateTag = stack.getOrCreateTagElement("BlockStateTag");
            blockStateTag.putString("lit", "true");
            helper.accept(stack);
        }
    }
}
