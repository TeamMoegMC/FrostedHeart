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

package com.teammoeg.frostedheart.content.tips.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.tips.ServerTipHelper;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DisplayCustomTipRequestPacket(Tip tip) implements CMessage {

    public DisplayCustomTipRequestPacket(FriendlyByteBuf buffer) {
        this(TipHelper.parse(buffer.readNbt()));
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        Tip.CODEC.encodeStart(NbtOps.INSTANCE, tip);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null) {
            if (!player.hasPermissions(2)) {
                var message = player.getName().getString() + " attempted to send tip to all players but did not have sufficient permission level";
                FHMain.LOGGER.warn(message);
                ServerTipHelper.sendCustomToAllOps(Tip.builder("warning").contents(message, "Tip contents:", "").contents(tip.contents()).pin(true).alwaysVisible(true).build());
                ServerTipHelper.sendCustom(Tip.builder("warning").contents("HACKER!").pin(true).alwaysVisible(true).build(), player);
            } else {
                context.get().enqueueWork(() -> ServerTipHelper.sendCustomToAll(tip));
            }
        }
        context.get().setPacketHandled(true);
    }
}
