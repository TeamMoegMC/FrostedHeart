package com.teammoeg.frostedheart.content.scenario.runner;

import com.teammoeg.frostedheart.util.TranslateUtils;

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
		player.displayClientMessage(TranslateUtils.str(string), false);
	}

	@Override
	public Player player() {
		return player;
	}

	@Override
	public String getLang() {
		if(lang==null) {
			lang=player.getLanguage();
		}
		return lang;
	}
	public ScenarioConductor conductor() {
		return conductor;
	}

	public void setPlayer(ServerPlayer player) {
		this.player = player;
	}

}
