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

package com.teammoeg.frostedheart.content.steamenergy.steamcore;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.teammoeg.chorda.block.CBlockInterfaces;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.climate.render.TemperatureGoogleRenderer;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetwork;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetworkProvider;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class SteamCoreTileEntity extends GeneratingKineticBlockEntity implements
        CTickableBlockEntity, IHaveGoggleInformation,
        CBlockInterfaces.IActiveState, HeatNetworkProvider {
    HeatEndpoint network = new HeatEndpoint(10, FHConfig.SERVER.STEAM_CORE.steamCoreMaxPower.get().floatValue(), 0, FHConfig.SERVER.STEAM_CORE.steamCorePowerIntake.get().floatValue());
    LazyOptional<HeatEndpoint> heatcap = LazyOptional.of(() -> network);

    public SteamCoreTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.setLazyTickRate(20);
    }
    float generatingSpeed=0;
    public float getGeneratedSpeed() {
        return generatingSpeed;
    }

    public float calculateAddedStressCapacity() {
        return this.lastCapacityProvided =FHConfig.SERVER.STEAM_CORE.steamCoreCapacity.get().floatValue();
    }

    @Override
    public void tick() {
        super.tick();
        
        if (!level.isClientSide) {
            if (network.tryDrainHeat(FHConfig.SERVER.STEAM_CORE.steamCorePowerIntake.get().floatValue())) {
            	float targetSpeed=FHConfig.SERVER.STEAM_CORE.steamCoreGeneratedSpeed.get().floatValue();
                if (generatingSpeed !=targetSpeed) {
                	generatingSpeed=targetSpeed;
                	this.setActive(true);
                	this.updateGeneratedRotation();
                }
                
                setChanged();
            } else if(generatingSpeed!=0){
            	generatingSpeed=0;
            	this.updateGeneratedRotation();
            	this.setActive(false);
            	setChanged();
            }
        }
    }


    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == FHCapabilities.HEAT_EP.capability() && side == this.getBlockState().getValue(BlockStateProperties.FACING).getOpposite()) {
            return heatcap.cast();
        }
        return super.getCapability(cap, side);
    }

    public Direction getDirection() {
        return this.getBlockState().getValue(BlockStateProperties.FACING);
    }


    @Override
    protected void write(CompoundTag tag, boolean client) {
        super.write(tag, client);
        network.save(tag, client);
        tag.putFloat("generatingSpeed", generatingSpeed);
    }

    @Override
    public BlockState getBlock() {
        return this.getBlockState();
    }

    @Override
    public void setBlock(BlockState blockState) {
        if (this.getWorldNonnull().getBlockState(this.worldPosition) == this.getBlock()) {
            this.getWorldNonnull().setBlockAndUpdate(this.worldPosition, blockState);
            super.setBlockState(blockState);
        }
    }

    public Level getWorldNonnull() {
        return Objects.requireNonNull(super.getLevel());
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        network.load(compound, clientPacket);
        generatingSpeed=compound.getFloat("generatingSpeed");
    }
	@Override
	public void invalidateCaps() {
		heatcap.invalidate();
		super.invalidateCaps();
	}

    @Override
    public @Nullable HeatNetwork getNetwork() {
        return network.getNetwork();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return TemperatureGoogleRenderer.addHeatNetworkInfoToTooltip(tooltip, isPlayerSneaking, worldPosition);
    }
}
