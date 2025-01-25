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

package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.chorda.io.NBTSerializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * An abstract Endpoint for a Heat Network.
 */
@Getter
@ToString()
public class HeatEndpoint implements NBTSerializable, HeatNetworkProvider {

    /**
     * The main network.
     */
    @ToString.Exclude
    HeatNetwork network;
    /**
     * The distance of this endpoint to network center.
     * This value is only for measuring detach priority, not penalty would be apply for long distance
     */
    int distance = -1;
    /**
     * Temperature level of the network.
     * <p>
     * This is normally defined by the Generator, or a Heat Debugger.
     */
    int tempLevel = 1;
    /**
     * The max capacity to store heat.
     */
    final float capacity;
    /**
     * Detach priority.
     * If power is low, endpoint with lower priority would detach first
     */
    @Getter
    final int priority;
    /**
     * Current heat stored.
     * Sometimes it is referred to as "power".
     */
    @Setter
    float heat = 0;
    /**
     * The Block and Position of this endpoint.
     * These are maintained by the network, so we default them here.
     */
    Block blk = Blocks.AIR;
    BlockPos pos = BlockPos.ZERO;
    /**
     * The heat intake from the network at this tick.
     */
    float intake = 0;
    /**
     * The heat output to the network at this tick.
     */
    float output = 0;
    /**
     * The recent average intake from the network maintained by the network.
     */
    float avgIntake = 0;
    /**
     * The recent average output to the network maintained by the network.
     */
    float avgOutput = 0;
    /**
     * The maximum intake when receiving heat from the network.<br>
     */
    @Getter @Setter
    float maxIntake = 0;
    /**
     * The maximum heat output of this provider when providing heat to the network.<br>
     */
    @Getter @Setter
    float maxOutput = 0;
    /**
     * Whether the recent average intake is less than the max intake.
     * Can receive more.
     */
    boolean canCostMore = false;
    /**
     * Is working even when Chunk is unloaded.
     */
    @Setter
    private boolean persist;
    /**
     * Is chunk unloaded.
     */
    @Getter
    private boolean unloaded;
    public HeatEndpoint(Block block, BlockPos pos, int priority, float capacity) {
        this.blk = block;
        this.pos = pos;
        this.priority = priority;
        this.capacity = capacity;
    }

    public HeatEndpoint(Block block, BlockPos pos, float capacity) {
        this.blk = block;
        this.pos = pos;
        this.priority = 0;
        this.capacity = capacity;
    }

    public HeatEndpoint() {
        this.priority = 0;
        this.capacity = 0;
    }

    /**
     * Instantiates HeatProviderEndPoint.<br>
     *
     * @param priority  if power is low, endpoint with lower priority would detach first
     * @param capacity  the max power to store<br>
     * @param maxOutput the max heat put to network<br>
     * @param maxIntake the max heat requested from network<br>
     */
    public HeatEndpoint(int priority, float capacity, float maxOutput, float maxIntake) {
        this.priority = priority;
        this.capacity = Math.max(Math.max(maxIntake, maxOutput), capacity);
        this.maxOutput = maxOutput;
        this.maxIntake = maxIntake;
    }

    public HeatEndpoint(float capacity, float maxOutput, float maxIntake) {
        this.priority = 0;
        this.capacity = Math.max(Math.max(maxIntake, maxOutput), capacity);
        this.maxOutput = maxOutput;
        this.maxIntake = maxIntake;
    }

    public HeatEndpoint(float maxOutput, float maxIntake) {
        this.priority = 0;
        this.capacity = 4 * Math.max(maxIntake, maxOutput);
        this.maxOutput = maxOutput;
        this.maxIntake = maxIntake;
    }

    /**
     * Receive connection from network.
     *
     * @param level       current world
     * @param pos     current pos
     * @param manager the network
     * @param dist    the distance to central
     * @return true, if the endpoint should receive connection
     */
    public boolean reciveConnection(Level level, BlockPos pos, HeatNetwork manager, Direction dir, int dist) {
    	if(this.network!=null) {
    		if(network.getNetworkSize()<=manager.getNetworkSize()) {
    			this.network=null;
    			this.distance=-1;
    			manager.removeEndpoint(this,level, pos, dir);
    		}
    	}
        return true;//manager.addEndpoint(this, dist, level, pos);
    }

    /**
     * The network calls this method to provide information about the connection
     * This should only be called by network.
     *
     * @param network  the network
     * @param distance the distance
     */
    public void setConnectionInfo(HeatNetwork network, int distance, BlockPos pos, Level level) {
        this.network = network;
        this.distance = distance;
        this.pos = pos;
        this.blk = level.getBlockState(pos).getBlock();
    }

    /**
     * Clear current connection.
     */
    public void clearConnection() {
        this.network = null;
        this.distance = -1;
    }

    /**
     * Can receive heat from the network.
     * <p>
     * The network would call this to check if this is a consumer.
     * If this returns true, this endpoint would be added to the consumer list with or without actually consume.
     * The network may also put heat into this endpoint.
     * <p>
     * This should be only called by the network, You should not call this method.
     *
     * @return true, if successful
     */
    protected boolean canReceiveHeatFromNetwork() {
        return maxIntake > 0 && heat < capacity;
    }

    /**
     * Receive heat from the network.
     * <p>
     * If the heat provided lesser than max intake then the heat statistics would show red
     * <p>
     * This should be only called by the network, You should not call this method.
     *
     * @param filled the amount of heat that the network fills to this endpoint
     * @param level  the actual temperature level, which my differ from HeatEndpoint#tempLevel
     * @return the amount of heat actually filled
     */
    protected float receiveHeatFromNetwork(float filled, int level) {
        float required = Math.min(maxIntake, capacity - heat);
        tempLevel = level;
        if (required > 0) {
            if (filled >= required) {
                filled -= required;
                heat += required;
                return required;
            }
            heat += filled;
            return filled;
        }
        return 0;
    }

    /**
     * Whether this endpoint can provide heat to the network.
     * <p>
     * The network would call this to check if this is a provider.
     * If this returns true, this endpoint would be added to the generator list with or without actually generate.
     * <p>
     * This should be only called by the network, You should not call this method.
     *
     * @return true, if successful
     */
    protected boolean canProvideHeatToNetwork() {
        return maxOutput > 0 && heat > 0;
    }

    /**
     * Provide heat to the network.
     * <p>
     * This should be only called by the network, You should not call this method.
     *
     * @return the amount of heat actually provided
     */
    protected float provideHeatToNetwork() {
        float provided = Math.min(heat, maxOutput);
        heat -= provided;
        return provided;
    }

    /**
     * Adds heat to the endpoint, if capacity exceed, tbe remaining would be disposed.
     * The heat actually added to the endpoint still depends on the generation.
     */
    public void addHeat(float added) {
        heat = Math.min(capacity, heat + added);
    }

    /**
     * Fill the endpoint with heat.
     */
    public void fill() {
        heat = capacity;
    }

    public void clear() {
        heat = 0;
    }

    /**
     * Drain heat from this endpoint.
     *
     * @param val the heat value to drain
     * @return the heat actually drain
     */
    public float drainHeat(float val) {
        float drained = Math.min(heat, val);
        heat -= drained;
        return drained;
    }

    /**
     * Try drain heat from this endpoint if there is enough heat.
     *
     * @param val the amount of heat to drain
     * @return if the heat is drained successfully
     */
    public boolean tryDrainHeat(float val) {
        if (heat >= val) {
            heat -= val;
            return true;
        }
        return false;
    }

    /**
     * Checks if the network valid.
     *
     * @return true, if successful
     */
    public boolean hasValidNetwork() {
        return network != null;
    }
    /**
     * This method is called by the network each tick to calculate average stats
     * <p>
     * This should be only called by the network, You should not call this method.
     * */
    protected void pushData() {
        avgIntake = avgIntake * .95f + Math.max(0, intake) * .05f;
        avgOutput = avgOutput * .95f + Math.max(0, output) * .05f;
        canCostMore = Math.round(avgIntake * 10) / 10f < maxIntake;
    }

    public void writeNetwork(FriendlyByteBuf pb) {
        pb.writeRegistryIdUnsafe(ForgeRegistries.BLOCKS, blk);
        pb.writeBlockPos(pos);
        pb.writeInt(priority);
        pb.writeFloat(capacity);
        pb.writeFloat(avgIntake);
        pb.writeFloat(avgOutput);
        pb.writeBoolean(canCostMore);
        pb.writeFloat(maxIntake);
        pb.writeFloat(maxOutput);
    }

    public static HeatEndpoint readNetwork(FriendlyByteBuf pb) {
        HeatEndpoint dat = new HeatEndpoint(
                pb.readRegistryIdUnsafe(ForgeRegistries.BLOCKS),
                pb.readBlockPos(),
                pb.readInt(),
                pb.readFloat()
        );
        dat.avgIntake = pb.readFloat();
        dat.avgOutput = pb.readFloat();
        dat.canCostMore = pb.readBoolean();
        dat.maxIntake = pb.readFloat();
        dat.maxOutput = pb.readFloat();
        return dat;
    }

    @Override
    public boolean equals(Object obj) {
        // check blk, pos, priority, capacity, avgIntake, avgOutput, canCostMore, maxIntake, maxOutput
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        HeatEndpoint that = (HeatEndpoint) obj;
        return blk.equals(that.blk) && pos.equals(that.pos) && priority == that.priority && Float.compare(that.capacity, capacity) == 0 && Float.compare(that.avgIntake, avgIntake) == 0 && Float.compare(that.avgOutput, avgOutput) == 0 && canCostMore == that.canCostMore && Float.compare(that.maxIntake, maxIntake) == 0 && Float.compare(that.maxOutput, maxOutput) == 0;
    }

    public void load(CompoundTag nbt, boolean isPacket) {
        heat = nbt.getFloat("net_power");
        //pos = BlockPos.of(nbt.getLong("pos"));
        //blk = CRegistries.getBlock(new ResourceLocation(nbt.getString("block")));
    }

    public void save(CompoundTag nbt, boolean isPacket) {
        nbt.putFloat("net_power", heat);
        //nbt.putLong("pos", pos.asLong());
       //nbt.putString("block", CRegistries.getRegistryName(blk).toString());
    }
    @Override
    public HeatNetwork getNetwork() {
        return network;
    }
    public void unload() {
    	this.unloaded=true;
    }
}
