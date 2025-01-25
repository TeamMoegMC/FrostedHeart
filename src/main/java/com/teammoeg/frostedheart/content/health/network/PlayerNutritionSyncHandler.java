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

package com.teammoeg.frostedheart.content.health.network;

import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerNutritionSyncHandler {

    public static void handle(Supplier<NetworkEvent.Context> context, PlayerNutritionSyncPacket packet){
        context.get().enqueueWork(() -> NutritionCapability.getCapability(Minecraft.getInstance().player).ifPresent(data -> {
            data.set(packet.getNutrition());
        }));
        context.get().setPacketHandled(true);

    }
}
