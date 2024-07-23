package com.teammoeg.frostedheart.content.scenario.runner;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.scenario.network.ServerSenarioActPacket;
import com.teammoeg.frostedheart.content.scenario.network.ServerSenarioScenePacket;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * A scene is a place to present content to client You should NOT store this
 * object, always get it from {@link ScenarioConductor#getScene()}
 */
public class ServerScene extends Scene {
	public ServerScene() {
		super();
	}

	@Override
	protected void sendScene(IScenarioThread parent,String text, boolean wrap, boolean reset) {
		FHNetwork.send(PacketDistributor.PLAYER.with(() -> ((ServerPlayer)parent.getPlayer())), new ServerSenarioScenePacket(text, wrap, isNowait, reset,parent.getStatus(),isClick));
		isClick=true;
	}
	@Override
	public void sendTitles(IScenarioThread parent,String title,String subtitle) {
		FHNetwork.send(PacketDistributor.PLAYER.with(()->((ServerPlayer)parent.getPlayer())), new ServerSenarioActPacket(title,subtitle));
	}

}
