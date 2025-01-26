/*
 * Copyright (c) 2024 TeamMoeg
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

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.scenario.network.S2CSenarioActPacket;
import com.teammoeg.frostedheart.content.scenario.network.S2CSenarioScenePacket;

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
	protected void sendScene(ScenarioContext ctx,ScenarioThread thread,String text,RunStatus status,boolean wrap, boolean reset,boolean waitClick) {
		FHNetwork.send(PacketDistributor.PLAYER.with(() -> ((ServerPlayer)ctx.player())), new S2CSenarioScenePacket(thread.getRunId(),text, wrap, isNowait, reset,status,waitClick));
	}
	@Override
	public void sendTitles(ScenarioContext ctx,ScenarioThread thread,String title,String subtitle) {
		FHNetwork.send(PacketDistributor.PLAYER.with(()->((ServerPlayer)ctx.player())), new S2CSenarioActPacket(title,subtitle));
	}

}
