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

package com.teammoeg.frostedheart.content.water.network;

import com.teammoeg.chorda.network.CMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerWaterLevelSyncPacket implements CMessage {
    int waterLevel, waterSaturationLevel;
    float waterExhaustionLevel;

    public PlayerWaterLevelSyncPacket(FriendlyByteBuf buffer) {
        waterLevel = buffer.readInt();
        waterSaturationLevel = buffer.readInt();
        waterExhaustionLevel = buffer.readFloat();
    }

    public PlayerWaterLevelSyncPacket(int waterLevel, int waterSaturationLevel, float waterExhaustionLevel) {
        this.waterLevel = waterLevel;
        this.waterSaturationLevel = waterSaturationLevel;
        this.waterExhaustionLevel = waterExhaustionLevel;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(waterLevel);
        buffer.writeInt(waterSaturationLevel);
        buffer.writeFloat(waterExhaustionLevel);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        if (context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            PlayerWaterLevelHandler.handle(context, this);
        }
    }
}
