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

package com.teammoeg.frostedheart.content.climate.player;

import net.minecraft.world.phys.shapes.VoxelShape;

/** Rules for compacting immutable block simulation results into shared values. */
final class BlockInfoCachePolicy {

    private BlockInfoCachePolicy() {
    }

    /**
     * 空碰撞形状只有在温度也为零时才等价于空气；火焰等空碰撞热源必须保温。
     * <p>
     * An empty collision shape is equivalent to air only at zero temperature;
     * heated empty shapes such as fire must retain their temperature.
     */
    static boolean canReuseAirInfo(VoxelShape shape, float temperature) {
        return shape.isEmpty() && temperature == 0f;
    }
}
