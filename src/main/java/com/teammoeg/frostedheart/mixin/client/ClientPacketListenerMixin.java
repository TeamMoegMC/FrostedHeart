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

package com.teammoeg.frostedheart.mixin.client;

import com.teammoeg.frostedheart.clusterserver.ClientConnectionHelper;
import com.teammoeg.frostedheart.clusterserver.ServerConnectionHelper;
import com.teammoeg.frostedheart.content.climate.player.thermalitem.WarmStoneGateBPacketCounter;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(at = @At("HEAD"), method = "setTitleText", cancellable = true)
    public void fh$setTitleText(ClientboundSetTitleTextPacket pPacket, CallbackInfo cbi) {
        String text = pPacket.getText().getString();
        if (text.startsWith(ServerConnectionHelper.HEADER)) {//start mark
            int code = text.codePointAt(2) & 0xFF;
            switch (code) {
                case 0: ClientConnectionHelper.token = text.substring(3); break;
                case 1: ClientConnectionHelper.back(); break;
                case 2: ClientConnectionHelper.joinNewServer(text.substring(3), false); break;
                case 3: ClientConnectionHelper.joinNewServer(text.substring(3), true); break;
            }
            cbi.cancel();
        }
    }

    @Inject(at = @At("TAIL"), method = "handleContainerSetSlot")
    private void fh$handleContainerSetSlot(
            ClientboundContainerSetSlotPacket packet,
            CallbackInfo callbackInfo
    ) {
        WarmStoneGateBPacketCounter.onContainerSlotPacket(
                packet.getContainerId(), packet.getSlot(), packet.getItem());
    }

    @Inject(at = @At("TAIL"), method = "handleContainerContent")
    private void fh$handleContainerContent(
            ClientboundContainerSetContentPacket packet,
            CallbackInfo callbackInfo
    ) {
        WarmStoneGateBPacketCounter.onContainerContentPacket(
                packet.getContainerId(), packet.getItems());
    }
}
