/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.content.generator;

import com.teammoeg.frostedheart.content.steamenergy.ISteamEnergyBlock;

import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import net.minecraftforge.fml.RegistryObject;

public class HeatedGeneratorMultiBlock<T extends MultiblockPartTileEntity<? super T>> extends NormalGeneratorMultiBlock<T> implements ISteamEnergyBlock {

    public HeatedGeneratorMultiBlock(String name, RegistryObject type) {
        super(name, type);
    }


    public HeatedGeneratorMultiBlock(String name, Properties props, RegistryObject type) {
        super(name, props, type);
    }


}
