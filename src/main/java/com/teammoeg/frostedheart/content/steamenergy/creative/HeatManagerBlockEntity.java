/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.steamenergy.creative;

import javax.annotation.Nonnull;

import com.teammoeg.frostedheart.content.steamenergy.ConnectorNetworkRevalidator;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetwork;
import com.teammoeg.frostedheart.content.steamenergy.NetworkConnector;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;

public class HeatManagerBlockEntity extends HeatBlockEntity implements NetworkConnector {
    HeatNetwork manager;
    ConnectorNetworkRevalidator<HeatManagerBlockEntity> networkHandler = new ConnectorNetworkRevalidator<>(this);
    public HeatManagerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        manager = new HeatNetwork( () -> {
            for (Direction d : Direction.values()) {
                manager.connectTo(level, worldPosition.relative(d),getBlockPos(), d.getOpposite());
            }
        });
        endpoint = HeatEndpoint.provider(-1, Integer.MAX_VALUE);
        heatcap = LazyOptional.of(() -> endpoint);
    }

    @Override
    public void tick() {
        super.tick();
        if(!endpoint.hasValidNetwork())
            manager.addEndpoint(heatcap.cast(), 0, getLevel(), getBlockPos());
        manager.tick(level);
        networkHandler.tick();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        manager.save(tag, false);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        manager.load(tag, false);
    }

    @Override
    @Nonnull
    public HeatNetwork getNetwork() {
        return networkHandler.hasNetwork()?networkHandler.getNetwork():manager;
    }

    @Override
    public boolean canConnectTo(Direction to) {
        return true;
    }

    @Override
    public void setNetwork(HeatNetwork network) {
        networkHandler.setNetwork(network);
    }

    @Override
    public void destroy() {
        super.destroy();
        manager.invalidate(level);
    }
}
