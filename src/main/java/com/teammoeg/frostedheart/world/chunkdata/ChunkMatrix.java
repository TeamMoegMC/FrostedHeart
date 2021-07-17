package com.teammoeg.frostedheart.world.chunkdata;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.INBTSerializable;

/*
 * 16x16x128 byte matrix containing temperature for every block in a chunk.
 */
public class ChunkMatrix implements INBTSerializable<CompoundNBT> {
    private byte[][][] matrix = new byte[16][16][256];
    // x, z, y


    public byte[][][] getMatrix() {
        return matrix;
    }

    public void setMatrix(byte[][][] mat) {
        matrix = mat;
    }

    public void setValue(int x, int y, int z, byte value) {
        matrix[x][z][y] = value;
    }

    public byte getValue(int x, int y, int z) {
        return matrix[x][z][y];
    }

    public byte getTemperature(BlockPos pos) {
        int y = pos.getY();
        int x = pos.getX() < 0 ? 15 + pos.getX() % 16 : pos.getX() % 16;
        int z = pos.getZ() < 0 ? 15 + pos.getZ() % 16 : pos.getZ() % 16;
        if (y >= 0 && y < 256) {
            return matrix[x][z][y];
        } else if (y < 0) {
            return matrix[x][z][0];
        } else {
            return matrix[x][z][255];
        }
    }

    public void addTemp(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, byte tempMod) {
        if (fromY < 0) {
            fromY = 0;
        } else if (fromY > 255) {
            fromY = 255;
            toY = 255;
        }

        if (toY > 255) {
            toY = 255;
        } else if (toY < 0) {
            toY = 0;
            fromY = 0;
        }

        for (int x = fromX; x < toX; x++)
            for (int z = fromZ; z < toZ; z++)
                for (int y = fromY; y < toY; y++) {
                    matrix[x][z][y] += tempMod;
                }
    }

    public void setTemp(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, byte newTemp) {
        if (fromY < 0) {
            fromY = 0;
        } else if (fromY > 255) {
            fromY = 255;
            toY = 255;
        }

        if (toY > 255) {
            toY = 255;
        } else if (toY < 0) {
            toY = 0;
            fromY = 0;
        }

        for (int x = fromX; x < toX; x++)
            for (int z = fromZ; z < toZ; z++)
                for (int y = fromY; y < toY; y++) {
                    matrix[x][z][y] = newTemp;
                }
    }

    public ChunkMatrix(PacketBuffer buffer) {
        deserialize(buffer);
    }

    public ChunkMatrix(byte defaultValue) {
        init(defaultValue);
    }

    public void init(byte defaultValue) {
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++)
                for (int y = 0; y < 256; y++) {
                    matrix[x][z][y] = defaultValue;
                }
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                nbt.putByteArray("x" + x + "z" + z, matrix[x][z]);
            }
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                matrix[x][z] = nbt.getByteArray("x" + x + "z" + z);
            }
    }

    public void serialize(PacketBuffer buffer) {
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                buffer.writeByteArray(matrix[x][z]);
            }
    }

    public void deserialize(PacketBuffer buffer) {
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                matrix[x][z] = buffer.readByteArray();
            }
    }
}
