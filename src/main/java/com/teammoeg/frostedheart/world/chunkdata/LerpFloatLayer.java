/*
 * Original work by AlcatrazEscapee
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package com.teammoeg.frostedheart.world.chunkdata;

import com.teammoeg.frostedheart.world.noise.NoiseUtil;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * This is a simple linearly interpolated float grid.
 * It records the value at the corner, and interpolates the values between on demand.
 */
@Deprecated
public class LerpFloatLayer implements INBTSerializable<CompoundNBT>
{
    private float valueNW, valueNE, valueSW, valueSE;

    public LerpFloatLayer(PacketBuffer buffer)
    {
        deserialize(buffer);
    }

    public LerpFloatLayer(float defaultValue)
    {
        init(defaultValue, defaultValue, defaultValue, defaultValue);
    }

    public void init(float valueNW, float valueNE, float valueSW, float valueSE)
    {
        this.valueNW = valueNW;
        this.valueNE = valueNE;
        this.valueSW = valueSW;
        this.valueSE = valueSE;
    }

    /**
     * Gets the floating point value approximated at the point within the grid.
     *
     * @param tNS A distance in the N-S direction. 0 = Full north, 1 = Full south.
     * @param tEW A distance in the E-W direction. 0 = Full east, 1 = Full west.
     */
    public float getValue(float tNS, float tEW)
    {
        return NoiseUtil.lerpGrid(valueNE, valueNW, valueSE, valueSW, tNS, tEW);
    }

    @Override
    public CompoundNBT serializeNBT()
    {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putFloat("nw", valueNW);
        nbt.putFloat("ne", valueNE);
        nbt.putFloat("sw", valueSW);
        nbt.putFloat("se", valueSE);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt)
    {
        valueNW = nbt.getFloat("nw");
        valueNE = nbt.getFloat("ne");
        valueSW = nbt.getFloat("sw");
        valueSE = nbt.getFloat("se");
    }

    public void serialize(PacketBuffer buffer)
    {
        buffer.writeFloat(valueNW);
        buffer.writeFloat(valueNE);
        buffer.writeFloat(valueSW);
        buffer.writeFloat(valueSE);
    }

    public void deserialize(PacketBuffer buffer)
    {
        valueNW = buffer.readFloat();
        valueNE = buffer.readFloat();
        valueSW = buffer.readFloat();
        valueSE = buffer.readFloat();
    }
}