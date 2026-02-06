/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.agriculture;

import org.jetbrains.annotations.Nullable;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerGrade;
import com.teammoeg.frostedheart.content.agriculture.Fertilizer.FertilizerType;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

public class FertilizedDirt extends Block {
    public static final EnumProperty<FertilizerType> FERTILIZER = EnumProperty.create("fertilizer",FertilizerType.class);
    public static final IntegerProperty STORAGE = IntegerProperty.create("storage", 1, 8);
    public static final EnumProperty<FertilizerGrade> GRADE = EnumProperty.create("grade",FertilizerGrade.class);
    public FertilizedDirt(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FERTILIZER, FertilizerType.ACCELERATED).setValue(GRADE, FertilizerGrade.BASIC).setValue(STORAGE, 1));
    }

	@Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FERTILIZER, GRADE, STORAGE);
    }

	@Override
	public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction toolAction,
			boolean simulate) {
		if (ToolActions.HOE_TILL == toolAction) {
			return FHBlocks.FERTILIZED_FARMLAND.getDefaultState().setValue(FertilizedDirt.FERTILIZER, state.getValue(FertilizedDirt.FERTILIZER))
					.setValue(FertilizedDirt.STORAGE, state.getValue(FertilizedDirt.STORAGE)).setValue(FertilizedDirt.GRADE, state.getValue(FertilizedDirt.GRADE));
			
		}else
		return super.getToolModifiedState(state, context, toolAction, simulate);
	}
}
