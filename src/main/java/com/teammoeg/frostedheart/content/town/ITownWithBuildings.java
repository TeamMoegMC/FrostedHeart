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

package com.teammoeg.frostedheart.content.town;

import com.teammoeg.frostedheart.content.town.block.TownBlockEntity;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.Optional;

public interface ITownWithBuildings extends Town{
    Map<BlockPos, AbstractTownBuilding> getTownBuildings();

    default Optional<AbstractTownBuilding> getTownBuilding(BlockPos pos){
        return Optional.ofNullable(getTownBuildings().get(pos));
    }

    void addTownBlock(BlockPos pos, TownBlockEntity<?> tile);

    void removeTownBlock(ServerLevel sl,BlockPos pos);
}