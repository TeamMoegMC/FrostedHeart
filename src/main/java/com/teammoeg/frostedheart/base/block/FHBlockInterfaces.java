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

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import net.minecraft.block.BlockState;
import net.minecraft.state.properties.BlockStateProperties;

public class FHBlockInterfaces {
    public interface IActiveState extends IEBlockInterfaces.BlockstateProvider {
        default boolean getIsActive() {
            BlockState state = this.getState();
            return state.hasProperty(BlockStateProperties.LIT) ? (Boolean) state.get(BlockStateProperties.LIT) : false;
        }

        /**
         * Set the block to active or inactive.
         * @param active true if the block should be active, false otherwise
         * @return true if the state was changed, false otherwise
         */
        default boolean setActive(boolean active) {
            BlockState state = this.getState();
            if (state.get(BlockStateProperties.LIT) != active) {
                BlockState newState = state.with(BlockStateProperties.LIT, active);
                this.setState(newState);
                return true;
            }
            return false;
        }
    }

    public FHBlockInterfaces() {
    }
}
