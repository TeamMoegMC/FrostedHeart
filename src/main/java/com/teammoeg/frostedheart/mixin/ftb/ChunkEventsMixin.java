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

package com.teammoeg.frostedheart.mixin.ftb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftbchunks.net.SendManyChunksPacket;
import dev.ftb.mods.ftbteams.event.PlayerJoinedPartyTeamEvent;
import net.minecraft.command.CommandSource;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.world.World;

@Mixin(FTBChunks.class)
public class ChunkEventsMixin {

    public ChunkEventsMixin() {
    }

    /**
     * @author khjxiaogu
     * @reason TODO
     */
    @Overwrite(remap = false)
    private void playerJoinedParty(PlayerJoinedPartyTeamEvent event) {
        CommandSource sourceStack = event.getTeam().manager.server.getCommandSource();
        FTBChunksTeamData oldData = FTBChunksAPI.getManager().getData(event.getPreviousTeam());
        FTBChunksTeamData newData = FTBChunksAPI.getManager().getData(event.getTeam());
        newData.updateLimits(event.getPlayer());

        Map<RegistryKey<World>, List<SendChunkPacket.SingleChunk>> chunksToSend = new HashMap<>();
        Map<RegistryKey<World>, List<SendChunkPacket.SingleChunk>> chunksToUnclaim = new HashMap<>();
        int chunks = 0;
        long now = System.currentTimeMillis();
        int total = newData.getClaimedChunks().size();

        for (ClaimedChunk chunk : oldData.getClaimedChunks()) {
            if (total >= newData.maxClaimChunks) {
                chunk.unclaim(sourceStack, false);
                chunksToUnclaim.computeIfAbsent(chunk.pos.dimension, s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, chunk.pos.x, chunk.pos.z, null));
            } else {
                oldData.manager.claimedChunks.remove(chunk.pos);
                oldData.save();
                chunk.teamData = newData;
                newData.manager.claimedChunks.put(chunk.pos, chunk);
                newData.save();
                chunksToSend.computeIfAbsent(chunk.pos.dimension, s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, chunk.pos.x, chunk.pos.z, chunk));
            }
            chunks++;

            total++;
        }

        if (chunks == 0) {
            return;
        }

        for (Map.Entry<RegistryKey<World>, List<SendChunkPacket.SingleChunk>> entry : chunksToSend.entrySet()) {
            SendManyChunksPacket packet = new SendManyChunksPacket();
            packet.dimension = entry.getKey();
            packet.teamId = newData.getTeamId();
            packet.chunks = entry.getValue();
            packet.sendToAll(sourceStack.getServer());
        }

        for (Map.Entry<RegistryKey<World>, List<SendChunkPacket.SingleChunk>> entry : chunksToUnclaim.entrySet()) {
            SendManyChunksPacket packet = new SendManyChunksPacket();
            packet.dimension = entry.getKey();
            packet.teamId = Util.DUMMY_UUID;
            packet.chunks = entry.getValue();
            packet.sendToAll(sourceStack.getServer());
        }

        FTBChunks.LOGGER.info("Transferred " + chunks + "/" + total + " chunks from " + oldData + " to " + newData);
    }
}
