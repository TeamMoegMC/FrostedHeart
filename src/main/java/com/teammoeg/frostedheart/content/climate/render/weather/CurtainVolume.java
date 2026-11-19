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

package com.teammoeg.frostedheart.content.climate.render.weather;

import com.teammoeg.frostedheart.content.climate.gamedata.climate.WhiteCurtainFieldModel.VisualKernel;

/** Horizontal extent of the snowy part of a travelling event, in world blocks. */
final class CurtainVolume {
    double minX, minZ, maxX, maxZ;

    /** Clips both moving boundaries to the corridor. A passed leading edge can still leave deep snow behind. */
    boolean set(VisualKernel kernel, double seconds) {
        if (kernel.firstSnowHour < 0) return false;
        double leading = kernel.leadingSnowDeltaChunks(seconds) * 16.0;
        double trailing = kernel.trailingSnowDeltaChunks(seconds) * 16.0;
        minX = kernel.minBlockX;
        maxX = kernel.maxBlockXExclusive;
        minZ = kernel.minBlockZ;
        maxZ = kernel.maxBlockZExclusive;
        if (kernel.moveX > 0) {
            double origin = minX + 8.0;
            minX = Math.max(minX, origin + trailing);
            maxX = Math.min(maxX, origin + leading);
        } else if (kernel.moveX < 0) {
            double origin = maxX - 8.0;
            minX = Math.max(minX, origin - leading);
            maxX = Math.min(maxX, origin - trailing);
        } else if (kernel.moveZ > 0) {
            double origin = minZ + 8.0;
            minZ = Math.max(minZ, origin + trailing);
            maxZ = Math.min(maxZ, origin + leading);
        } else {
            double origin = maxZ - 8.0;
            minZ = Math.max(minZ, origin - leading);
            maxZ = Math.min(maxZ, origin - trailing);
        }
        return minX < maxX && minZ < maxZ;
    }
}
