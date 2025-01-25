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

package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceType;
import com.teammoeg.frostedheart.content.town.TownWorker;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import net.minecraft.nbt.CompoundTag;

public class WarehouseWorker implements TownWorker {
    @Override
    public boolean firstWork(Town town, CompoundTag workData) {
        double capacity = workData.getCompound("tileEntity").getDouble("capacity");
        town.getResourceManager().addIfHaveCapacity(VirtualResourceType.MAX_CAPACITY.generateKey(0), capacity);
        return true;

    }

    @Override
    public boolean work(Town town, CompoundTag workData) {
        return false;
    }
}
