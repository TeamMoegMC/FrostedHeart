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
        ClientUtils.mc().clearLevel();
        ClientUtils.mc().prepareForMultiplayer();
        ClientUtils.mc().updateReportEnvironment(ReportEnvironment.thirdParty(pServerData != null ? pServerData.ip : "localhost"));
        ClientUtils.mc().quickPlayLog().setWorldData(QuickPlayLog.Type.MULTIPLAYER, pServerData.ip, pServerData.name);
		super.handleGameProfile(pPacket);
	}

}
