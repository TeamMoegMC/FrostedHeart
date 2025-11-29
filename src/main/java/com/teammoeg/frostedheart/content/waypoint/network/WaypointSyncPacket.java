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

package com.teammoeg.frostedheart.content.waypoint.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.waypoint.ClientWaypointManager;
import com.teammoeg.frostedheart.content.waypoint.WaypointManager;
import com.teammoeg.frostedheart.content.waypoint.waypoints.AbstractWaypoint;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record WaypointSyncPacket(AbstractWaypoint waypoint) implements CMessage {

    public WaypointSyncPacket(FriendlyByteBuf buffer) {
        this(WaypointManager.registry.read(buffer));
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        WaypointManager.registry.write(buffer, waypoint);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                ClientWaypointManager.putWaypointWithoutSendingPacket(waypoint);
            } else {
                WaypointManager.getManager(context.get().getSender()).putWaypointWithoutSendingPacket(waypoint);
            }
        });
        context.get().setPacketHandled(true);
    }
}
