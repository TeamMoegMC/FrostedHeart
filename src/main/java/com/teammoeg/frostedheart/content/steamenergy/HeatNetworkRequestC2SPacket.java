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

package com.teammoeg.frostedheart.content.steamenergy;

import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockBE;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.chorda.network.CMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HeatNetworkRequestC2SPacket implements CMessage {

    private final BlockPos pos; // the position of the network constituent (pipe or endpoint) player looking at


    public HeatNetworkRequestC2SPacket(BlockPos pos) {
        this.pos = pos;
    }

    public HeatNetworkRequestC2SPacket(FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Get the data needed on server side
            var player = context.get().getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                BlockEntity be = level.getBlockEntity(pos);

                if (be instanceof IMultiblockBE multiblockBE) {
                    IMultiblockState mbState = multiblockBE.getHelper().getState();
                    if (mbState instanceof HeatNetworkProvider hp) {
                        HeatNetwork network = hp.getNetwork();
                        if (network != null) {
//                        FHMain.LOGGER.debug("Client request received. Sending server HeatNetwork data to client");
                            ClientHeatNetworkData data = new ClientHeatNetworkData(pos, network);
                            FHNetwork.INSTANCE.sendPlayer(player, new HeatNetworkResponseS2CPacket(data));
                            return;
                        }
                    }
                }else if (be instanceof HeatNetworkProvider hp) {
                    HeatNetwork network = hp.getNetwork();
                    if (network != null) {
//                        FHMain.LOGGER.debug("Client request received. Sending server HeatNetwork data to client");
                        ClientHeatNetworkData data = new ClientHeatNetworkData(pos, network);
                        FHNetwork.INSTANCE.sendPlayer(player, new HeatNetworkResponseS2CPacket(data));
                        return;
                    }
                }
                FHNetwork.INSTANCE.sendPlayer(player, new HeatNetworkResponseS2CPacket(new ClientHeatNetworkData(pos)));
            }
        });
        context.get().setPacketHandled(true);
    }
}
