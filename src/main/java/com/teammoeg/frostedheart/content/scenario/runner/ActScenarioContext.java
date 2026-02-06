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

package com.teammoeg.frostedheart.content.scenario.runner;

import com.teammoeg.chorda.text.Components;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class ActScenarioContext extends ScenarioContext {
	ServerPlayer player;
	String lang;
	ScenarioConductor conductor;
	Scene scene;
	public ActScenarioContext(ScenarioConductor conductor,Scene scene) {
		super();
		this.conductor = conductor;
		this.scene=scene;
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

	@Override
	public Scene getScene() {
		return scene;
	}

}
