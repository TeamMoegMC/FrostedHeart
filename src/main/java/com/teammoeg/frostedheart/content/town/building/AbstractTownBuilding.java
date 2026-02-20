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

package com.teammoeg.frostedheart.content.town.building;

import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.block.OccupiedArea;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;

/**
 *
 */
@Getter
public abstract class AbstractTownBuilding implements ITownBuilding{


    /**
     * BlockPos of the main block of the building
     */
    @Getter
    protected final BlockPos pos;

    /**
     * 建筑最初创建时，默认状态为未初始化，检查能否工作时应优先检查此项，或许可以避免意外的空指针？
     */
    public boolean initialized = false;

    public boolean occupiedAreaOverlapped = false;

    public boolean isStructureValid = false;

    @Getter
    @Setter
    public OccupiedArea occupiedArea = OccupiedArea.EMPTY;

    protected AbstractTownBuilding(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public boolean isBuildingWorkable(){
        return initialized && !occupiedAreaOverlapped && isStructureValid;
    }

    public boolean work(Town town){
        return true;
    }

    public void onRemoved(Town town){

    }

}
