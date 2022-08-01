package com.teammoeg.frostedheart.world.structure;

import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.gen.feature.structure.StructurePiece;
import net.minecraft.world.gen.feature.template.TemplateManager;

import java.util.Random;


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

        this.boundingBox = new MutableBoundingBox(
                center.getX() - range, center.getY(), center.getZ() - range,
                center.getX() + range, center.getY() + 32, center.getZ() + range);
    }

    public VolcanicVentPiece(TemplateManager templateManager, CompoundNBT nbt) {
        super(null, nbt);
        this.centerX = nbt.getInt("x");
        this.centerY = nbt.getInt("y");
        this.centerZ = nbt.getInt("z");
    }

    protected void readAdditional(CompoundNBT nbt) {
        nbt.putInt("x", this.centerX);
        nbt.putInt("y", this.centerY);
        nbt.putInt("z", this.centerZ);
    }

    @Override
    public boolean func_230383_a_(ISeedReader reader, StructureManager structureManager, ChunkGenerator generator, Random seed, MutableBoundingBox boundingBox, ChunkPos chunkPos, BlockPos pos) {
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        for (int x = boundingBox.minX; x <= boundingBox.maxX; x++) {
            mutablePos.setX(x);
            for (int z = boundingBox.minZ; z <= boundingBox.maxZ; z++) {
                mutablePos.setZ(z);
                int distX = x - centerX;
                int distZ = z - centerZ;
                double dist = Math.sqrt(distX * distX + distZ * distZ);
                for (int y = centerY; y <= centerY + 32; y++) {
                    mutablePos.setY(y);
                    if (y < centerY + 32 - dist) {
                        reader.setBlockState(mutablePos, Blocks.BASALT.getDefaultState(), 2);
                    }
                }
            }
        }
        return true;
    }
}
