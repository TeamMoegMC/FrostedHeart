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

package com.teammoeg.chorda.block;

import com.teammoeg.chorda.block.entity.BlockStateAccess;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class CBlockInterfaces {
    public interface IActiveState extends BlockStateAccess {
        default boolean getIsActive() {
            BlockState state = this.getBlock();
            return state.hasProperty(BlockStateProperties.LIT) ? state.getValue(BlockStateProperties.LIT) : false;
        }

        /**
         * Set the block to active or inactive.
         * @param active true if the block should be active, false otherwise
         * @return true if the state was changed, false otherwise
         */
        default boolean setActive(boolean active) {
            BlockState state = this.getBlock();
            if (state.getValue(BlockStateProperties.LIT) != active) {
                BlockState newState = state.setValue(BlockStateProperties.LIT, active);
                this.setBlock(newState);
                return true;
            }
            return false;
        }
    }

    public CBlockInterfaces() {
    }
}
