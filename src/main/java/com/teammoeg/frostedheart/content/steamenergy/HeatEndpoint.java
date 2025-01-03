package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * An abstract Endpoint for A Heat Network.
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
     * Consumer priority, if power is low, endpoint with lower priority would detach first
     * -- GETTER --
     *  Gets the detach priority.
     *
     * @return the priority to detatch

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
    float intake = -1;
    /**
     * The heat output to the network at this tick.
     */
    float output = -1;
    /**
     * The recent average intake from the network maintained by the network.
     */
    float avgIntake = -1;
    /**
     * The recent average output to the network maintained by the network.
     */
    float avgOutput = -1;
    /**
     * The maximum intake when receiving heat.<br>
     * -- GETTER --
     *  The maximum heat to receive from the network.
     *
     * @return the max intake

     */
    @Getter
    float maxIntake = -1;
    /**
     * The maximum heat output of this provider.<br>
     * -- GETTER --
     *  The maximum heat to provide to the network.
     *
     * @return the max output

     */
    @Getter
    float maxOutput = -1;
    /**
     * Whether this endpoint receives more heat than it provides.
     */
    boolean canCostMore = false;

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

    public HeatEndpoint(int priority, float capacity) {
        this.priority = priority;
        this.capacity = capacity;
    }

    public HeatEndpoint(float capacity) {
        this.priority = 0;
        this.capacity = capacity;
    }

    public HeatEndpoint() {
        this.priority = 0;
        this.capacity = 0;
    }

    /**
     * Recive connection from network.
     *
     * @param level       current world
     * @param pos     current pos
     * @param manager the network
     * @param dist    the distance to central
     * @return true, if successful
     */
    public boolean reciveConnection(Level level, BlockPos pos, HeatNetwork manager, Direction dir, int dist) {
        return manager.addEndpoint(this, dist, level, pos);
    }

    /**
     * Connect to a network to distance.
     *
     * @param network  the network
     * @param distance the distance
     */
    public void connect(HeatNetwork network, int distance, BlockPos pos, Level level) {
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
     * The network may also put heat into this network.
     * <p>
     * This should be only called by the network, You should not call this method.
     *
     * @return true, if successful
     */
    public boolean canReceiveHeat() {
        return heat < capacity;
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
    protected float receiveHeat(float filled, int level) {
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
    public boolean canProvideHeat() {
        return heat > 0;
    }

    /**
     * Provide heat to the network.
     * <p>
     * This should be only called by the network, You should not call this method.
     *
     * @return the amount of heat actually provided
     */
    protected float provideHeat() {
        return 0;
    }

    /**
     * Checks if the network valid.
     *
     * @return true, if successful
     */
    public boolean hasValidNetwork() {
        return network != null;
    }

    public void pushData() {
        if (avgIntake < 0)
            avgIntake = intake;
        else
            avgIntake = avgIntake * .95f + Math.max(0, intake) * .05f;
        if (avgOutput < 0)
            avgOutput = output;
        else
            avgOutput = avgOutput * .95f + Math.max(0, output) * .05f;
        canCostMore = Math.round(avgOutput * 10) / 10f < maxIntake;
    }

    public void writeNetwork(FriendlyByteBuf pb) {
        pb.writeRegistryIdUnsafe(ForgeRegistries.BLOCKS, blk);
        pb.writeBlockPos(pos);
        pb.writeFloat(capacity);
        pb.writeFloat(avgIntake);
        pb.writeFloat(avgOutput);
        pb.writeBoolean(canCostMore);
    }

    public static HeatEndpoint readNetwork(FriendlyByteBuf pb) {
        HeatEndpoint dat = new HeatEndpoint(
                pb.readRegistryIdUnsafe(ForgeRegistries.BLOCKS),
                pb.readBlockPos(),
                pb.readFloat()
        );
        dat.avgIntake = pb.readFloat();
        dat.avgOutput = pb.readFloat();
        dat.canCostMore = pb.readBoolean();
        return dat;
    }

    public void load(CompoundTag nbt, boolean isPacket) {
        heat = nbt.getFloat("net_power");
        pos = BlockPos.of(nbt.getLong("pos"));
        blk = RegistryUtils.getBlock(new ResourceLocation(nbt.getString("block")));
    }

    public void save(CompoundTag nbt, boolean isPacket) {
        nbt.putFloat("net_power", heat);
        nbt.putLong("pos", pos.asLong());
        nbt.putString("block", RegistryUtils.getRegistryName(blk).toString());
    }

    @Override
    public HeatNetwork getNetwork() {
        return network;
    }
}
