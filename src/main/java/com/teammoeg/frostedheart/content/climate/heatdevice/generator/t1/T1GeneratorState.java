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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorState;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class T1GeneratorState extends GeneratorState {

    /**
     * The last position where the machine it supports is
     */
    BlockPos lastSupportPos;

    public T1GeneratorState() {
        super();
    }

    @Override
    public void writeSaveNBT(CompoundTag nbt) {
        super.writeSaveNBT(nbt);
        if (lastSupportPos != null)
            nbt.putLong("support", lastSupportPos.asLong());
    }

    @Override
    public void readSaveNBT(CompoundTag nbt) {
        super.readSaveNBT(nbt);
        lastSupportPos = null;
        if (nbt.contains("support"))
            lastSupportPos = BlockPos.of(nbt.getLong("support"));
    }

}
