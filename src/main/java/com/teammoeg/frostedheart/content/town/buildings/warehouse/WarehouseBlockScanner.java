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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.frostedheart.content.town.block.blockscanner.BuildingBlockScanner;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Getter
public class WarehouseBlockScanner extends BuildingBlockScanner {
    public WarehouseBlockScanner(Level world, BlockPos startPos) {
        super(world, startPos);
    }


    public boolean scan(){
        return super.scan();
    }
}
