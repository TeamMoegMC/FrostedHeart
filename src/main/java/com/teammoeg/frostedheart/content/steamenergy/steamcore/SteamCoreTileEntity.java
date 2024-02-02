package com.teammoeg.frostedheart.content.steamenergy.steamcore;

import java.util.Objects;

import javax.annotation.Nullable;

import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.content.steamenergy.INetworkConsumer;
import com.teammoeg.frostedheart.content.steamenergy.SteamNetworkConsumer;
import com.teammoeg.frostedheart.content.steamenergy.SteamNetworkHolder;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

public class SteamCoreTileEntity extends GeneratingKineticTileEntity implements
        INetworkConsumer, ITickableTileEntity, IHaveGoggleInformation,
        FHBlockInterfaces.IActiveState{
    public SteamCoreTileEntity() {
        super(FHTileTypes.STEAM_CORE.get());
        this.setLazyTickRate(20);
    }

    SteamNetworkConsumer network = new SteamNetworkConsumer(FHConfig.SERVER.steamCoreMaxPower.get().floatValue(),FHConfig.SERVER.steamCorePowerIntake.get().floatValue()*1.5f);

    public float getGeneratedSpeed(){
        float speed = FHConfig.SERVER.steamCoreGeneratedSpeed.get().floatValue();
        if(getIsActive()) return speed;
        return 0f;
    }

    public float calculateAddedStressCapacity() {
        if(getIsActive()) return FHConfig.SERVER.steamCoreCapacity.get().floatValue();
        return 0f;
    }

    @Override
    public void tick() {
        super.tick();
        if (!world.isRemote) {
            if (network.isValid()) {
                network.tick();
            }
            if(network.tryDrainHeat(FHConfig.SERVER.steamCorePowerIntake.get().floatValue())){
                this.setActive(true);
                if(this.getSpeed() == 0f){
                    this.updateGeneratedRotation();
                }
                markDirty();
            }else {
                this.setActive(false);
                this.updateGeneratedRotation();
            }
        }else {
            if(getIsActive()){
                ClientUtils.spawnSteamParticles(this.getWorld(), pos);
            }
        }
    }

    public void lazyTick() {
        super.lazyTick();

    }

    @Override
    public boolean connect(Direction to, int dist) {
        return network.reciveConnection(world, pos, to, dist);
    }

    @Override
    public boolean canConnectAt(Direction dir) {
        return dir == this.getBlockState().get(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
    }

    @Nullable
    @Override
    public SteamNetworkHolder getHolder() {
        return network;
    }

    public void drawEffect(int type) {
        if (world != null && world.isRemote) {
            if(type == 1)
                ClientUtils.spawnFireParticles(world, this.getPos());
            else
                ClientUtils.spawnSteamParticles(world, this.getPos());
        }
    }
    public Direction getDirection() {
        return this.getBlockState().get(BlockStateProperties.FACING);
    }

    @Override
    protected void fromTag(BlockState state, CompoundNBT tag, boolean client) {
        super.fromTag(state, tag, client);
        network.load(tag);
    }

    @Override
    protected void write(CompoundNBT tag, boolean client) {
        super.write(tag, client);
        network.save(tag);
    }

    @Override
    public BlockState getState() {
        return this.getBlockState();
    }

    @Override
    public void setState(BlockState blockState) {
        if (this.getWorldNonnull().getBlockState(this.pos) == this.getState()) {
            this.getWorldNonnull().setBlockState(this.pos, blockState);
        }
    }

    public World getWorldNonnull() {
        return (World) Objects.requireNonNull(super.getWorld());
    }
}
