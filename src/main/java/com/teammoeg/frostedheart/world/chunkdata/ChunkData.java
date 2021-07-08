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

public class ChunkData implements ICapabilitySerializable<CompoundNBT> {
    public static final ChunkData EMPTY = new Immutable();

    public static ChunkData get(IWorld world, BlockPos pos) {
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
    public static ChunkData get(IWorld world, ChunkPos pos) {
        // Query cache first, picking the correct cache for the current logical side
        ChunkData data = ChunkDataCache.get(world).get(pos);
        if (data == null) {
            return getCapability(world.chunkExists(pos.x, pos.z) ? world.getChunk(pos.asBlockPos()) : null).orElse(ChunkData.EMPTY);
        }
        return data;
    }

    /**
     * Helper method, since lazy optionals and instanceof checks together are ugly
     */
    public static LazyOptional<ChunkData> getCapability(@Nullable IChunk chunk) {
        if (chunk instanceof Chunk) {
            return ((Chunk) chunk).getCapability(ChunkDataCapability.CAPABILITY);
        }
        return LazyOptional.empty();
    }

    /**
     * Used on a ServerWorld context to change current ChunkData for both client and server side cache.
     */
    public static void changeChunkData(IWorld world, ChunkPos chunkPos, ChunkMatrix layer) {
        if (world != null && !world.isRemote()) {
            IChunk chunk = world.chunkExists(chunkPos.x, chunkPos.z) ? world.getChunk(chunkPos.x, chunkPos.z) : null;
            ChunkData data = ChunkData.getCapability(chunk).map(
                    dataIn -> {
                        ChunkDataCache.SERVER.update(chunkPos, dataIn);
                        ChunkDataCache.CLIENT.update(chunkPos, dataIn);
                        return dataIn;
                    }).orElseGet(() -> ChunkDataCache.SERVER.getOrCreate(chunkPos));
            data.setChunkMatrix(layer);
        }
    }

    public static void brushChunk(IWorld world, ChunkPos chunkPos, int fromX, int fromY, int fromZ, int toX, int toY, int toZ, byte tempMod) {
        if (world != null && !world.isRemote()) {
            IChunk chunk = world.chunkExists(chunkPos.x, chunkPos.z) ? world.getChunk(chunkPos.x, chunkPos.z) : null;
            ChunkData data = ChunkData.getCapability(chunk).map(
                    dataIn -> {
                        ChunkDataCache.SERVER.update(chunkPos, dataIn);
                        ChunkDataCache.CLIENT.update(chunkPos, dataIn);
                        return dataIn;
                    }).orElseGet(() -> ChunkDataCache.SERVER.getOrCreate(chunkPos));
            data.chunkMatrix.brush(fromX, fromY, fromZ, toX, toY, toZ, tempMod);
        }
    }

    private static int getChunkRelativePos(int actualPos) {
        return actualPos < 0 ? actualPos % 16 + 16 : actualPos % 16;
    }

    public static void update(IWorld world, BlockPos heatPos, int range, byte tempMod) {
        int sourceX = heatPos.getX(), sourceY = heatPos.getY(), sourceZ = heatPos.getZ();
        int sourceRelativeX = getChunkRelativePos(sourceX);
        int sourceRelativeZ = getChunkRelativePos(sourceZ);

        // these are block position offset
        int offsetN = sourceZ - range;
        int offsetS = sourceZ + range;
        int offsetW = sourceX - range;
        int offsetE = sourceX + range;

        // these are chunk position offset
        int chunkOffsetW = offsetW < 0 ? offsetW / 16 - 1 : offsetW / 16;
        int chunkOffsetE = offsetE < 0 ? offsetE / 16 - 1 : offsetE / 16;
        int chunkOffsetN = offsetN < 0 ? offsetN / 16 - 1 : offsetN / 16;
        int chunkOffsetS = offsetS < 0 ? offsetS / 16 - 1 : offsetS / 16;

        boolean hasInnerChunks = true, hasWESide = true, hasNSSide = true, hasNSCorners = true, hasWECorners = true;
        int innerChunkOffsetW = chunkOffsetW + 1;
        int innerChunkOffsetE = chunkOffsetE - 1;
        int innerChunkOffsetN = chunkOffsetN + 1;
        int innerChunkOffsetS = chunkOffsetS - 1;

        if (innerChunkOffsetW > innerChunkOffsetE) {
            hasWESide = false;
            if (chunkOffsetW == chunkOffsetE) {
                hasWECorners = false;
            }
        }
        if (innerChunkOffsetN > innerChunkOffsetS) {
            hasNSSide = false;
            if (chunkOffsetN == chunkOffsetS) {
                hasNSCorners = false;
            }
        }
        if (!hasWESide && !hasNSSide) {
            hasInnerChunks = false;
        }

        int upperLimit = sourceY + range;
        int lowerLimit = sourceY - range;

        // inner chunks
        if (hasInnerChunks) {
            for (int x = innerChunkOffsetW; x <= innerChunkOffsetE; x++)
                for (int z = innerChunkOffsetN; z <= innerChunkOffsetS; z++)
                    brushChunk(world, new ChunkPos(x, z), 0, lowerLimit, 0, 16, upperLimit, 16, tempMod);
        }

        // side chunks
        if (hasNSSide) {
            for (int z = innerChunkOffsetN; z <= innerChunkOffsetS; z++) {
                // west side
                brushChunk(world, new ChunkPos(chunkOffsetW, z), getChunkRelativePos(offsetW), lowerLimit,0, 16, upperLimit, 16, tempMod);
                // east side
                brushChunk(world, new ChunkPos(chunkOffsetE, z), 0, lowerLimit,0, getChunkRelativePos(offsetE), upperLimit, 16, tempMod);
            }
        }
        if (hasWESide) {
            for (int x = innerChunkOffsetW; x <= innerChunkOffsetE; x++) {
                // north side
                brushChunk(world, new ChunkPos(x, chunkOffsetN), 0, lowerLimit, getChunkRelativePos(offsetN), 16, upperLimit, 16, tempMod);
                // south side
                brushChunk(world, new ChunkPos(x, chunkOffsetS), 0, lowerLimit,0, 16, upperLimit, getChunkRelativePos(offsetS), tempMod);
            }
        }

        // corner chunks
        if (hasWECorners && hasNSCorners) {
            // northwest
            brushChunk(world, new ChunkPos(chunkOffsetW, chunkOffsetN), getChunkRelativePos(offsetW), lowerLimit, getChunkRelativePos(offsetN), 16, upperLimit, 16, tempMod);
            // northeast
            brushChunk(world, new ChunkPos(chunkOffsetE, chunkOffsetN), 0, lowerLimit, getChunkRelativePos(offsetN), getChunkRelativePos(offsetE), upperLimit, 16, tempMod);
            // southwest
            brushChunk(world, new ChunkPos(chunkOffsetW, chunkOffsetS), getChunkRelativePos(offsetW), lowerLimit, 0, 16, upperLimit, getChunkRelativePos(offsetS), tempMod);
            // southeast
            brushChunk(world, new ChunkPos(chunkOffsetE, chunkOffsetS), 0, lowerLimit, 0, getChunkRelativePos(offsetE), upperLimit, getChunkRelativePos(offsetS), tempMod);
        } else {
            if (!hasWECorners && hasNSCorners) {
                // north (W or E is either Ok here)
                brushChunk(world, new ChunkPos(chunkOffsetW, chunkOffsetN), getChunkRelativePos(offsetW), lowerLimit, getChunkRelativePos(offsetN), getChunkRelativePos(offsetE), upperLimit, 16, tempMod);
                // south
                brushChunk(world, new ChunkPos(chunkOffsetW, chunkOffsetS), getChunkRelativePos(offsetW), lowerLimit, 0, getChunkRelativePos(offsetE), upperLimit, getChunkRelativePos(offsetS), tempMod);
            }
            if (hasWECorners && !hasNSCorners) {
                // west
                brushChunk(world, new ChunkPos(chunkOffsetW, chunkOffsetN), getChunkRelativePos(offsetW), lowerLimit, getChunkRelativePos(offsetN), 16, upperLimit, getChunkRelativePos(offsetS), tempMod);
                // east
                brushChunk(world, new ChunkPos(chunkOffsetE, chunkOffsetN), 0, lowerLimit, getChunkRelativePos(offsetN), getChunkRelativePos(offsetE), upperLimit, getChunkRelativePos(offsetS), tempMod);
            }
            if (!hasWECorners && !hasNSCorners) {
                // single (W or E, N or S)
                brushChunk(world, new ChunkPos(chunkOffsetE, chunkOffsetN), getChunkRelativePos(offsetW), lowerLimit, getChunkRelativePos(offsetN), getChunkRelativePos(offsetE), upperLimit, getChunkRelativePos(offsetS), tempMod);
            }
        }
    }

    private final LazyOptional<ChunkData> capability;
    private final ChunkPos pos;

    private Status status;

    private ChunkMatrix chunkMatrix;

    public ChunkData(ChunkPos pos) {
        this.pos = pos;
        this.capability = LazyOptional.of(() -> this);

        reset();
    }

    public void setChunkMatrix(ChunkMatrix chunkMatrix) {
        this.chunkMatrix = chunkMatrix;
    }

    public ChunkPos getPos() {
        return pos;
    }

    public float getTemperatureAtBlock(BlockPos pos) {
        return chunkMatrix.getTemperature(pos);
    }

    public void initChunkMatrix ( byte defaultValue){
        chunkMatrix.init(defaultValue);
    }

    public Status getStatus ()
    {
        return status;
    }

    public void setStatus (Status status)
    {
        this.status = status;
    }

    /**
     * @return If the current chunk data is empty, then return other
     */
    public ChunkData ifEmptyGet (Supplier < ChunkData > other)
    {
        return status != Status.EMPTY ? this : other.get();
    }

    /**
     * Create an update packet to send to client with necessary information
     */
    public ChunkWatchPacket getUpdatePacket ()
    {
        return new ChunkWatchPacket(pos.x, pos.z, chunkMatrix);
    }

    /**
     * Called on client, sets to received data
     */
    public void onUpdatePacket (ChunkMatrix chunkMatrix)
    {
        this.chunkMatrix = chunkMatrix;

        if (status == Status.CLIENT || status == Status.EMPTY) {
            this.status = Status.CLIENT;
        } else {
            throw new IllegalStateException("ChunkData#onUpdatePacket was called on non client side chunk data: " + this);
        }
    }

    @Override
    public <T > LazyOptional < T > getCapability(Capability < T > cap, @Nullable Direction side)
    {
        return ChunkDataCapability.CAPABILITY.orEmpty(cap, capability);
    }

    @Override
    public CompoundNBT serializeNBT ()
    {
        CompoundNBT nbt = new CompoundNBT();

        nbt.putByte("status", (byte) status.ordinal());
        if (status.isAtLeast(Status.CLIMATE)) {
            nbt.put("temperature", chunkMatrix.serializeNBT());
        }
        return nbt;
    }

    @Override
    public void deserializeNBT (CompoundNBT nbt)
    {
        if (nbt != null) {
            status = Status.valueOf(nbt.getByte("status"));
            if (status.isAtLeast(Status.CLIMATE)) {
                chunkMatrix.deserializeNBT(nbt.getCompound("temperature"));
            }
        }
    }

    @Override
    public String toString ()
    {
        return "ChunkData{pos=" + pos + ", status=" + status + ", hashCode=" + Integer.toHexString(hashCode()) + '}';
    }

    private void reset ()
    {
        chunkMatrix = new ChunkMatrix((byte) 10);
        status = Status.EMPTY;
    }

    public enum Status {
        CLIENT, // Special status - indicates it is a client side shallow copy
        EMPTY, // Empty - default. Should never be called to generate.
        CLIMATE; // Climate data, rainfall and temperature

        private static final Status[] VALUES = values();

        public static Status valueOf(int i) {
            return i >= 0 && i < VALUES.length ? VALUES[i] : EMPTY;
        }

        public Status next() {
            return VALUES[this.ordinal() + 1];
        }

        public boolean isAtLeast(Status otherStatus) {
            return this.ordinal() >= otherStatus.ordinal();
        }
    }

    /**
     * Only used for the empty instance, this will enforce that it never leaks data
     * New empty instances can be constructed via constructor, EMPTY instance is specifically for an immutable empty copy, representing invalid chunk data
     */
    private static final class Immutable extends ChunkData {
        private Immutable() {
            super(new ChunkPos(ChunkPos.SENTINEL));
        }

        @Override
        public void initChunkMatrix(byte defaultValue) {
            throw new UnsupportedOperationException("Tried to modify immutable chunk data");
        }

        @Override
        public void setStatus(Status status) {
            throw new UnsupportedOperationException("Tried to modify immutable chunk data");
        }

        @Override
        public void onUpdatePacket(ChunkMatrix temperatureLayer) {
            throw new UnsupportedOperationException("Tried to modify immutable chunk data");
        }

        @Override
        public void deserializeNBT(CompoundNBT nbt) {
            throw new UnsupportedOperationException("Tried to modify immutable chunk data");
        }

        @Override
        public String toString() {
            return "ImmutableChunkData";
        }
    }
}

