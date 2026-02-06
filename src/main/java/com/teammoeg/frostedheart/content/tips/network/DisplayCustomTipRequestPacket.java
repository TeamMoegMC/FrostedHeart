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

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.content.tips.ServerTipSender;
import com.teammoeg.frostedheart.content.tips.Tip;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DisplayCustomTipRequestPacket(Tip tip) implements CMessage {

    public DisplayCustomTipRequestPacket(FriendlyByteBuf buffer) {
        this(Tip.builder("").fromNBT(buffer.readNbt()).build());
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        tip.write(buffer);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        var player = context.get().getSender();
        if (player != null) {
            if (!player.hasPermissions(2)) {
                FHMain.LOGGER.warn("{} IS A HACKER!", player.getName().getString());
                ServerTipSender.sendCustom(Tip.builder("warning").line(Components.str("HACKER!")).pin(true).alwaysVisible(true).build(), player);
            } else {
                context.get().enqueueWork(() -> ServerTipSender.sendCustomToAll(tip));
            }
        }
        context.get().setPacketHandled(true);
    }
}
