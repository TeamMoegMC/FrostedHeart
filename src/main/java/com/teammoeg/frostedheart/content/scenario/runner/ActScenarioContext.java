package com.teammoeg.frostedheart.content.scenario.runner;

import com.teammoeg.chorda.util.lang.Components;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class ActScenarioContext extends ScenarioContext {
	ServerPlayer player;
	String lang;
	ScenarioConductor conductor;

	public ActScenarioContext(ScenarioConductor conductor) {
		super();
		this.conductor = conductor;
	}

	@Override
	public void sendMessage(String string) {
		player.displayClientMessage(Components.str(string), false);
	}

	@Override
	public Player player() {
		return player;
	}

	@Override
	public String getLang() {
		return lang;
	}
	public ScenarioConductor conductor() {
		return conductor;
	}

	public void setPlayerAndLang(ServerPlayer player,String lang) {
		this.player = player;
		if(lang!=null)
		this.lang=lang;
	}

}
