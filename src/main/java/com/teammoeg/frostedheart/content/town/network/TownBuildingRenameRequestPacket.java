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

import java.util.function.Supplier;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TownNamingModel;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 玩家给自己城镇里的一座建筑改名；名字留空就恢复成「类型加编号」。改动随建筑一起同步给全队。
 * <p>
 * A player renames a building of their own town; a blank name restores "type and number". The change syncs to the
 * team with the building.
 */
public class TownBuildingRenameRequestPacket implements CMessage {
    private final BlockPos pos;
    private final String requestedName;

    public TownBuildingRenameRequestPacket(BlockPos pos, String requestedName) {
        this.pos = pos.immutable();
        this.requestedName = requestedName == null ? "" : requestedName;
    }

    public TownBuildingRenameRequestPacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.requestedName = buffer.readUtf(TownNamingModel.MAX_BUILDING_NAME_LENGTH * 4);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeUtf(requestedName, TownNamingModel.MAX_BUILDING_NAME_LENGTH * 4);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            String name = TownNamingModel.normalizeBuildingName(requestedName);
            CTeamDataManager.get(player).getOptional(FHSpecialDataTypes.TOWN_DATA).ifPresent(townData -> {
                AbstractTownBuilding building = townData.createTeamTown().getTownBuildings().get(pos);
                if (building != null) building.setCustomName(name.isEmpty() ? null : name);
            });
        });
        context.get().setPacketHandled(true);
    }
}
