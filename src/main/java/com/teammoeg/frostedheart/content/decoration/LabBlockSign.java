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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class LabBlockSign extends CBlock implements ICreativeModeTabItem {
    public static IntegerProperty SIGN = IntegerProperty.create("sign", 0, 4);
    public LabBlockSign(Properties blockProps) {
        super(blockProps);
        this.registerDefaultState(this.stateDefinition.any().setValue(SIGN, 0));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SIGN);
    }

    @Override
    public void fillItemCategory(CreativeTabItemHelper helper) {
        for (int value : SIGN.getPossibleValues()) {
            if (helper.isType(FHTabs.building_blocks)) {
                if (value == 0) continue;
                ItemStack stack = new ItemStack(this);
                CompoundTag blockStateTag = stack.getOrCreateTagElement("BlockStateTag");
                blockStateTag.putString(SIGN.getName(), String.valueOf(value));
                helper.accept(stack);
            }
        }
    }
/*    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Integer finalType = Math.abs(worldIn.random.nextInt()) % typeCount;
        BlockState newState = this.stateDefinition.any().setValue(NUMBER, finalType);
        worldIn.setBlockAndUpdate(pos, newState);
    }*/


}
