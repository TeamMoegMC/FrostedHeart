package com.teammoeg.frostedheart.content.scenario.runner;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.scenario.network.ServerSenarioActPacket;
import com.teammoeg.frostedheart.content.scenario.network.ServerSenarioScenePacket;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * A scene is a place to present content to client You should NOT store this
 * object, always get it from {@link ScenarioConductor#getScene()}
 */
public class ServerScene extends Scene {
	public ServerScene() {
		super();
	}

	@Override
	protected void sendScene(ScenarioContext ctx,String text,RunStatus status,boolean wrap, boolean reset,boolean waitClick) {
		FHNetwork.send(PacketDistributor.PLAYER.with(() -> ((ServerPlayer)ctx.player())), new ServerSenarioScenePacket(text, wrap, isNowait, reset,status,waitClick));
	}
	@Override
	public void sendTitles(ScenarioContext ctx,String title,String subtitle) {
		FHNetwork.send(PacketDistributor.PLAYER.with(()->((ServerPlayer)ctx.player())), new ServerSenarioActPacket(title,subtitle));
	}

}
