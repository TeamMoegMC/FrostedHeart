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

package com.teammoeg.frostedheart.compat.create;

import com.teammoeg.chorda.math.Colors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.Objects;

public interface IHaveOutlines {
    Collection<ShapeInfo> getOutlineShapes();

    record ShapeInfo(String name, AABB shape, int color, int displayTick) {
        
        public static ShapeInfo fromBlock(BlockGetter level, BlockPos pos, String name, int color, int displayTick) {
            var block = level.getBlockState(pos);
            AABB aabb = null;
            if (!block.isAir()) {
                var shape = block.getBlockSupportShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ());
                if (!shape.isEmpty()) {
                    aabb = shape.bounds();
                }
            }
            return of(name, aabb == null ? new AABB(pos) : aabb, color, displayTick);
        }

        public static ShapeInfo of(String name, AABB shape, int color, int displayTick) {
            return new ShapeInfo(name, shape, color, displayTick);
        }

        public static ShapeInfo of(String name, AABB shape) {
            return of(name, shape, Colors.themeColor(), 60);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ShapeInfo info = (ShapeInfo) o;
            return color == info.color && displayTick == info.displayTick && Objects.equals(name, info.name) && Objects.equals(shape, info.shape);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, shape.hashCode(), color, displayTick);
        }
    }
}
