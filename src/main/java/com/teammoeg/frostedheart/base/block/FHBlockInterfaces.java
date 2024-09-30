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

package com.teammoeg.frostedheart.base.block;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class FHBlockInterfaces {
    public interface IActiveState extends IEBlockInterfaces.BlockstateProvider {
        default boolean getIsActive() {
            BlockState state = this.getState();
            return state.hasProperty(BlockStateProperties.LIT) ? state.getValue(BlockStateProperties.LIT) : false;
        }

        /**
         * Set the block to active or inactive.
         * @param active true if the block should be active, false otherwise
         * @return true if the state was changed, false otherwise
         */
        default boolean setActive(boolean active) {
            BlockState state = this.getState();
            if (state.getValue(BlockStateProperties.LIT) != active) {
                BlockState newState = state.setValue(BlockStateProperties.LIT, active);
                this.setState(newState);
                return true;
            }
            return false;
        }
    }
    public interface IActiveStateLogic {
        default boolean getIsActive(IMultiblockContext<?> ctx) {
            BlockState state = ctx.getLevel().getBlockState(BlockPos.ZERO);
            return state.hasProperty(BlockStateProperties.LIT) ? state.getValue(BlockStateProperties.LIT) : false;
        }

        /**
         * Set the block to active or inactive.
         * @param active true if the block should be active, false otherwise
         * @return true if the state was changed, false otherwise
         */
        default boolean setActive(IMultiblockContext<?> ctx,BlockPos relpos,boolean active) {
            BlockState state = ctx.getLevel().getBlockState(relpos);
            if (state.getValue(BlockStateProperties.LIT) != active) {
                BlockState newState = state.setValue(BlockStateProperties.LIT, active);
                ctx.getLevel().setBlock(relpos, newState);
                return true;
            }
            return false;
        }
    }
    public FHBlockInterfaces() {
    }
}
