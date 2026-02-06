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

package com.teammoeg.frostedheart.clusterserver.network;

import java.time.Duration;
import java.util.function.Consumer;

import com.teammoeg.chorda.client.ClientUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;

public class InGameClientHandshakePacketListenerImpl extends ClientHandshakePacketListenerImpl {
	private ServerData pServerData;
	public InGameClientHandshakePacketListenerImpl(Connection pConnection, Minecraft pMinecraft, ServerData pServerData, Screen pParent, boolean pNewWorld, Duration pWorldLoadDuration,
		Consumer<Component> pUpdateStatus) {
		super(pConnection, pMinecraft, pServerData, pParent, pNewWorld, pWorldLoadDuration, pUpdateStatus);
		this.pServerData=pServerData;
	}

	@Override
	public void handleGameProfile(ClientboundGameProfilePacket pPacket) {
        ClientUtils.getMc().clearLevel();
        ClientUtils.getMc().prepareForMultiplayer();
        ClientUtils.getMc().updateReportEnvironment(ReportEnvironment.thirdParty(pServerData != null ? pServerData.ip : "localhost"));
        ClientUtils.getMc().quickPlayLog().setWorldData(QuickPlayLog.Type.MULTIPLAYER, pServerData.ip, pServerData.name);
		super.handleGameProfile(pPacket);
	}

}
