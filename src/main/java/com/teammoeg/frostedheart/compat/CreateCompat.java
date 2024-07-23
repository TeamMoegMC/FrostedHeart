/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.compat;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.block.BlockStressDefaults;
import com.simibubi.create.foundation.block.BlockStressValues;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.RegistryUtils;

import net.minecraft.world.level.block.Blocks;

public class CreateCompat {
    public static void init() {
        BlockStressDefaults.setDefaultImpact(AllBlocks.MECHANICAL_HARVESTER.getId(), 4.0);
        BlockStressDefaults.setDefaultImpact(AllBlocks.MECHANICAL_PLOUGH.getId(), 4.0);
        BlockStressDefaults.setDefaultImpact(AllBlocks.ANDESITE_FUNNEL.getId(), 4.0);
        BlockStressDefaults.setDefaultImpact(AllBlocks.BRASS_FUNNEL.getId(), 4.0);
        BlockStressDefaults.setDefaultImpact(RegistryUtils.getRegistryName(Blocks.DISPENSER), 4.0);
        BlockStressValues.registerProvider(FHMain.MODID, new FHStress());

    }
}
