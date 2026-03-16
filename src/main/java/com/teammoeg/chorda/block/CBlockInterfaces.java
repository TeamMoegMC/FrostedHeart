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

/**
 * 方块行为接口定义集合，包含用于控制方块活跃状态等行为的内部接口。
 * <p>
 * A collection of block behavior interface definitions. Contains inner interfaces
 * for controlling block behaviors such as active/inactive state management.
 */
public class CBlockInterfaces {

    /**
     * 活跃状态接口，用于管理方块的活跃/非活跃状态。
     * 使用 {@link BlockStateProperties#LIT} 属性来表示活跃状态。
     * <p>
     * Active state interface for managing a block's active/inactive state.
     * Uses the {@link BlockStateProperties#LIT} property to represent the active state.
     */
    public interface IActiveState extends BlockStateAccess {

        /**
         * 获取方块当前是否处于活跃状态。
         * <p>
         * Gets whether the block is currently in an active state.
         *
         * @return 如果方块处于活跃状态则返回 true / true if the block is active
         */
        default boolean getIsActive() {
            BlockState state = this.getBlock();
            return state.hasProperty(BlockStateProperties.LIT) ? state.getValue(BlockStateProperties.LIT) : false;
        }

        /**
         * 设置方块的活跃或非活跃状态。
         * <p>
         * Sets the block to active or inactive.
         *
         * @param active 是否设为活跃状态 / true if the block should be active, false otherwise
         * @return 如果状态发生了改变则返回 true / true if the state was changed, false otherwise
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

    /**
     * 默认构造函数。
     * <p>
     * Default constructor.
     */
    public CBlockInterfaces() {
    }
}
