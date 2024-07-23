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

package com.teammoeg.frostedheart.world.geology.volcanic;

import java.util.Random;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;


public class VolcanicVentPiece extends StructurePiece {
    protected int centerX;
    protected int centerZ;
    protected int centerY;

    public VolcanicVentPiece(BlockPos center) {
        super(null, 0);
        this.centerX = center.getX();
        this.centerY = center.getY();
        this.centerZ = center.getZ();

        int range = 4 * 8;

        this.boundingBox = new BoundingBox(
                center.getX() - range, center.getY(), center.getZ() - range,
                center.getX() + range, center.getY() + 32, center.getZ() + range);
    }

    public VolcanicVentPiece(StructureManager templateManager, CompoundTag nbt) {
        super(null, nbt);
        this.centerX = nbt.getInt("x");
        this.centerY = nbt.getInt("y");
        this.centerZ = nbt.getInt("z");
    }

    @Override
    public boolean postProcess(WorldGenLevel reader, StructureFeatureManager structureManager, ChunkGenerator generator, Random seed, BoundingBox boundingBox, ChunkPos chunkPos, BlockPos pos) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = boundingBox.x0; x <= boundingBox.x1; x++) {
            mutablePos.setX(x);
            for (int z = boundingBox.z0; z <= boundingBox.z1; z++) {
                mutablePos.setZ(z);
                int distX = x - centerX;
                int distZ = z - centerZ;
                double dist = Math.sqrt(distX * distX + distZ * distZ);
                for (int y = centerY; y <= centerY + 32; y++) {
                    mutablePos.setY(y);
                    if (y < centerY + 32 - dist) {
                        reader.setBlock(mutablePos, Blocks.BASALT.defaultBlockState(), 2);
                    }
                }
            }
        }
        return true;
    }

    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("x", this.centerX);
        nbt.putInt("y", this.centerY);
        nbt.putInt("z", this.centerZ);
    }
}
