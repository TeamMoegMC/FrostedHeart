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

package com.teammoeg.frostedheart.content.generator.t1;

import java.util.function.Consumer;

import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.generator.BurnerGeneratorTileEntity;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class T1GeneratorTileEntity extends BurnerGeneratorTileEntity<T1GeneratorTileEntity> {
    public T1GeneratorTileEntity.GeneratorData guiData = new T1GeneratorTileEntity.GeneratorData();

    public T1GeneratorTileEntity() {
        super(FHMultiblocks.GENERATOR, FHTileTypes.GENERATOR_T1.get(), false,1,2,1);
    }
	@Override
	public void forEachBlock(Consumer<T1GeneratorTileEntity> consumer) {
		for (int x = 0; x < 3; ++x)
            for (int y = 0; y < 4; ++y)
                for (int z = 0; z < 3; ++z) {
                    BlockPos actualPos = getBlockPosForPos(new BlockPos(x, y, z));
                    TileEntity te = Utils.getExistingTileEntity(world, actualPos);
                    if (te instanceof T1GeneratorTileEntity)
                        consumer.accept((T1GeneratorTileEntity) te);
                }
	}
}
