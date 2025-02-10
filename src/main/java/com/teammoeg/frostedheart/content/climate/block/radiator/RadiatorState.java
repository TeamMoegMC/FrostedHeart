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

package com.teammoeg.frostedheart.content.climate.block.radiator;

import blusunrize.immersiveengineering.api.multiblocks.blocks.util.StoredCapability;

import com.teammoeg.frostedheart.content.climate.block.generator.HeatingState;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import net.minecraft.nbt.CompoundTag;

public class RadiatorState extends HeatingState {

    HeatEndpoint network = new HeatEndpoint(100, 100, 0, 4);
    StoredCapability<HeatEndpoint> heatCap=new StoredCapability<>(network);
    @Override
    public void readSaveNBT(CompoundTag nbt) {
        super.readSaveNBT(nbt);
        network.deserializeNBT(nbt.getCompound("network"));
    }

    @Override
    public void writeSaveNBT(CompoundTag nbt) {
        super.writeSaveNBT(nbt);
        nbt.put("network", network.serializeNBT());
    }

    @Override
	public void writeSyncNBT(CompoundTag nbt) {
		super.writeSyncNBT(nbt);
		nbt.putBoolean("_", true);
	}

	@Override
    public int getDownwardRange() {
        return 1;
    }

    @Override
    public int getUpwardRange() {
        return 4;
    }

    @Override
    public int getRadius() {
        return 7;
    }
}
