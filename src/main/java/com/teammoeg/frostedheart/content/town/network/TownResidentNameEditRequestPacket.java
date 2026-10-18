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

package com.teammoeg.frostedheart.content.town.network;

import java.util.UUID;
import java.util.function.Supplier;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TownNamingModel;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Server-authoritative resident rename scoped to a resident in the sender's team town. */
public class TownResidentNameEditRequestPacket implements CMessage {
    private final UUID residentId;
    private final String firstName;
    private final String lastName;

    public TownResidentNameEditRequestPacket(UUID residentId, String firstName, String lastName) {
        this.residentId = residentId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public TownResidentNameEditRequestPacket(FriendlyByteBuf buffer) {
        this.residentId = buffer.readUUID();
        this.firstName = buffer.readUtf(TownNamingModel.MAX_RESIDENT_NAME_PART_LENGTH);
        this.lastName = buffer.readUtf(TownNamingModel.MAX_RESIDENT_NAME_PART_LENGTH);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(residentId);
        buffer.writeUtf(firstName, TownNamingModel.MAX_RESIDENT_NAME_PART_LENGTH);
        buffer.writeUtf(lastName, TownNamingModel.MAX_RESIDENT_NAME_PART_LENGTH);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            TownNamingModel.normalizeResidentName(firstName, lastName).ifPresent(name ->
                    CTeamDataManager.get(player).getOptional(FHSpecialDataTypes.TOWN_DATA)
                            .flatMap(town -> town.createTeamTown().getResident(residentId))
                            .ifPresent(resident -> {
                                resident.setFirstName(name.firstName());
                                resident.setLastName(name.lastName());
                            }));
        });
        context.get().setPacketHandled(true);
    }
}
