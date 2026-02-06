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

package com.teammoeg.frostedheart.content.town.hunting;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockEntity;
import com.teammoeg.frostedheart.content.town.TownWorkerStatus;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.content.town.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.content.town.blockscanner.ConfinedSpaceScanner;
import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class HuntingCampBlockEntity extends AbstractTownWorkerBlockEntity<WorkerState> {
    public HuntingCampBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.HUNTING_CAMP.get(),pos,state);
    }

    public boolean isStructureValid(WorkerState state){
        ConfinedSpaceScanner confinedSpaceScanner = new ConfinedSpaceScanner(this.level, worldPosition.above());
        return !confinedSpaceScanner.scan(256);
    }

    
    @Override
    public void refresh(WorkerState state) {
    	state.getOccupiedArea().add(BlockScanner.toColumnPos(worldPosition));
        if(state.status == TownWorkerStatus.OCCUPIED_AREA_OVERLAPPED) return;
        state.status = isStructureValid(state)?TownWorkerStatus.VALID:TownWorkerStatus.NOT_VALID;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public TownWorkerType getWorkerType() {
        return TownWorkerType.HUNTING_BASE;
    }


}
