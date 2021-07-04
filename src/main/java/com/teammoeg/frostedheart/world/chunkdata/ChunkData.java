/*
 * Original work by AlcatrazEscapee
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package com.teammoeg.frostedheart.world.chunkdata;

import com.teammoeg.frostedheart.network.ChunkWatchPacket;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class ChunkData implements ICapabilitySerializable<CompoundNBT>
{
    public static final ChunkData EMPTY = new Immutable();

    public static ChunkData get(IWorld world, BlockPos pos)
    {
        return get(world, new ChunkPos(pos));
    }

    /**
     * Called to get chunk data when a world context is available.
     * If on client, will query capability, falling back to cache, and send request packets if necessary
     * If on server, will either query capability falling back to cache, or query provider to generate the data.
     *
     * @see ChunkDataProvider#get(ChunkPos, Status) to directly force chunk generation, or if a world is not available
     * @see ChunkDataCache#get(ChunkPos) to directly access the cache
     */
    public static ChunkData get(IWorld world, ChunkPos pos)
    {
        // Query cache first, picking the correct cache for the current logical side
        ChunkData data = ChunkDataCache.get(world).get(pos);
        if (data == null)
        {
            return getCapability(world.chunkExists(pos.x, pos.z) ? world.getChunk(pos.asBlockPos()) : null).orElse(ChunkData.EMPTY);
        }
        return data;
    }

    /**
     * Helper method, since lazy optionals and instanceof checks together are ugly
     */
    public static LazyOptional<ChunkData> getCapability(@Nullable IChunk chunk)
    {
        if (chunk instanceof Chunk)
        {
            return ((Chunk) chunk).getCapability(ChunkDataCapability.CAPABILITY);
        }
        return LazyOptional.empty();
    }

    /**
     * Used on a ServerWorld context to change current ChunkData for both client and server side cache.
     */
    public static void changeChunkData(IWorld world, int chunkX, int chunkZ, LerpFloatLayer layer) {
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        if (world != null && !world.isRemote()) {
            IChunk chunk = world.chunkExists(chunkX, chunkZ) ? world.getChunk(chunkX, chunkZ) : null;
            ChunkData data = ChunkData.getCapability(chunk).map(
                    dataIn -> {
                        ChunkDataCache.SERVER.update(pos, dataIn);
                        ChunkDataCache.CLIENT.update(pos, dataIn);
                        return dataIn;
                    }).orElseGet(() -> ChunkDataCache.SERVER.getOrCreate(pos));
            data.setTemperatureLayer(layer);
        }
    }

    private final LazyOptional<ChunkData> capability;
    private final ChunkPos pos;

    private Status status;

    private LerpFloatLayer temperatureLayer;

    public ChunkData(ChunkPos pos)
    {
        this.pos = pos;
        this.capability = LazyOptional.of(() -> this);

        reset();
    }

    public void setTemperatureLayer(LerpFloatLayer temperatureLayer) {
        this.temperatureLayer = temperatureLayer;
    }

    public ChunkPos getPos()
    {
        return pos;
    }

    public float getAverageTemp(BlockPos pos)
    {
        return getAverageTemp(pos.getX() & 15, pos.getZ() & 15);
    }

    public float getAverageTemp(int x, int z)
    {
        return temperatureLayer.getValue(z / 16f, 1 - (x / 16f));
    }

    public void setAverageTemp(float tempNW, float tempNE, float tempSW, float tempSE)
    {
        temperatureLayer.init(tempNW, tempNE, tempSW, tempSE);
    }

    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    /**
     * @return If the current chunk data is empty, then return other
     */
    public ChunkData ifEmptyGet(Supplier<ChunkData> other)
    {
        return status != Status.EMPTY ? this : other.get();
    }

    /**
     * Create an update packet to send to client with necessary information
     */
    public ChunkWatchPacket getUpdatePacket()
    {
        return new ChunkWatchPacket(pos.x, pos.z, temperatureLayer);
    }

    /**
     * Called on client, sets to received data
     */
    public void onUpdatePacket(LerpFloatLayer temperatureLayer)
    {
        this.temperatureLayer = temperatureLayer;

        if (status == Status.CLIENT || status == Status.EMPTY)
        {
            this.status = Status.CLIENT;
        }
        else
        {
            throw new IllegalStateException("ChunkData#onUpdatePacket was called on non client side chunk data: " + this);
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side)
    {
        return ChunkDataCapability.CAPABILITY.orEmpty(cap, capability);
    }

    @Override
    public CompoundNBT serializeNBT()
    {
        CompoundNBT nbt = new CompoundNBT();

        nbt.putByte("status", (byte) status.ordinal());
        if (status.isAtLeast(Status.CLIMATE))
        {
            nbt.put("temperature", temperatureLayer.serializeNBT());
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt)
    {
        if (nbt != null)
        {
            status = Status.valueOf(nbt.getByte("status"));
            if (status.isAtLeast(Status.CLIMATE))
            {
                temperatureLayer.deserializeNBT(nbt.getCompound("temperature"));
            }
        }
    }

    @Override
    public String toString()
    {
        return "ChunkData{pos=" + pos + ", status=" + status + ", hashCode=" + Integer.toHexString(hashCode()) + '}';
    }

    private void reset()
    {
        temperatureLayer = new LerpFloatLayer(10);
        status = Status.EMPTY;
    }

    public enum Status
    {
        CLIENT, // Special status - indicates it is a client side shallow copy
        EMPTY, // Empty - default. Should never be called to generate.
        CLIMATE; // Climate data, rainfall and temperature

        private static final Status[] VALUES = values();

        public static Status valueOf(int i)
        {
            return i >= 0 && i < VALUES.length ? VALUES[i] : EMPTY;
        }

        public Status next()
        {
            return VALUES[this.ordinal() + 1];
        }

        public boolean isAtLeast(Status otherStatus)
        {
            return this.ordinal() >= otherStatus.ordinal();
        }
    }

    /**
     * Only used for the empty instance, this will enforce that it never leaks data
     * New empty instances can be constructed via constructor, EMPTY instance is specifically for an immutable empty copy, representing invalid chunk data
     */
    private static final class Immutable extends ChunkData
    {
        private Immutable()
        {
            super(new ChunkPos(ChunkPos.SENTINEL));
        }

        @Override
        public void setAverageTemp(float tempNW, float tempNE, float tempSW, float tempSE)
        {
            throw new UnsupportedOperationException("Tried to modify immutable chunk data");
        }

        @Override
        public void setStatus(Status status)
        {
            throw new UnsupportedOperationException("Tried to modify immutable chunk data");
        }

        @Override
        public void onUpdatePacket(LerpFloatLayer temperatureLayer)
        {
            throw new UnsupportedOperationException("Tried to modify immutable chunk data");
        }

        @Override
        public void deserializeNBT(CompoundNBT nbt)
        {
            throw new UnsupportedOperationException("Tried to modify immutable chunk data");
        }

        @Override
        public String toString()
        {
            return "ImmutableChunkData";
        }
    }
}