/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.network;

import com.teammoeg.frostedheart.capability.ITempForecastCapability;
import com.teammoeg.frostedheart.capability.TempForecastCapabilityProvider;
import com.teammoeg.frostedheart.util.FHUtils;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from server -> client every ServerWorld#tick()
 */
public class WeatherPacket {
    private final int thunderTime;
    private final int rainTime;
    private final int clearTime;

    public WeatherPacket(ServerWorld world) {
        this.thunderTime = world.serverWorldInfo.getThunderTime();
        this.rainTime = world.serverWorldInfo.getRainTime();
        this.clearTime = world.serverWorldInfo.getClearWeatherTime();
    }

    public WeatherPacket(PacketBuffer buffer) {
        this.thunderTime = buffer.readVarInt();
        this.rainTime = buffer.readVarInt();
        this.clearTime = buffer.readVarInt();
    }

    void encode(PacketBuffer buffer) {
        buffer.writeVarInt(thunderTime);
        buffer.writeVarInt(rainTime);
        buffer.writeVarInt(clearTime);
    }

    void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            World world = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> FHUtils::getWorld);
            if (world != null) {
                LazyOptional<ITempForecastCapability> cap = TempForecastCapabilityProvider.getCapability(world);
                cap.ifPresent((capability) -> {
                    capability.setRainTime(rainTime);
                    capability.setThunderTime(thunderTime);
                    capability.setClearTime(clearTime);
                });
            }
        });
        context.get().setPacketHandled(true);
    }
}