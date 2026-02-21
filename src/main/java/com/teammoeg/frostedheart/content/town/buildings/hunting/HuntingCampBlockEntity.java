/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.town.buildings.hunting;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.block.OccupiedArea;
import com.teammoeg.frostedheart.content.town.block.blockscanner.ConfinedSpaceScanner;

import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class HuntingCampBlockEntity extends AbstractTownBuildingBlockEntity<HuntingCampBuilding> {
    public HuntingCampBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.HUNTING_CAMP.get(),pos,state);
    }


    
    @Override
    public void refresh(@NotNull HuntingCampBuilding building) {
        scanStructure(building);
    }

    @Override
    public @Nullable HuntingCampBuilding getBuilding(AbstractTownBuilding abstractTownBuilding) {
        if(abstractTownBuilding instanceof HuntingCampBuilding){
            return (HuntingCampBuilding) abstractTownBuilding;
        }
        return null;
    }


    @Override
    public boolean scanStructure(HuntingCampBuilding building) {
        ConfinedSpaceScanner confinedSpaceScanner = new ConfinedSpaceScanner(this.level, worldPosition.above());
        if(!confinedSpaceScanner.scan(256)){//不密封，也就是露天
            building.occupiedArea = new OccupiedArea(Set.of(new ColumnPos(worldPosition.getX(), worldPosition.getZ())));
            return true;
        }
        return false;
    }

    @Override
    public @NotNull HuntingCampBuilding createBuilding() {
        return new HuntingCampBuilding(this.getBlockPos());
    }
}
