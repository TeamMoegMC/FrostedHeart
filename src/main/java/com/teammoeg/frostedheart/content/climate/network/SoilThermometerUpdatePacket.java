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

package com.teammoeg.frostedheart.content.climate.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.climate.render.TemperatureGoogleRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SoilThermometerUpdatePacket(float temperature) implements CMessage {

    public SoilThermometerUpdatePacket(FriendlyByteBuf buffer) {
        this(buffer.readFloat());
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(temperature);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> TemperatureGoogleRenderer.cachedTemperature = temperature);
        context.get().setPacketHandled(true);
    }
}
