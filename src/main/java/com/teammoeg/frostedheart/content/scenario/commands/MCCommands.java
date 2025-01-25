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

package com.teammoeg.frostedheart.content.scenario.commands;

import java.util.HashMap;
import java.util.Map;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.chorda.team.CTeamDataManager;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.content.scenario.EventTriggerType;
import com.teammoeg.frostedheart.content.scenario.Param;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteTarget;
import com.teammoeg.frostedheart.content.scenario.runner.trigger.MovementTrigger;
import com.teammoeg.frostedheart.content.scenario.runner.trigger.OrTrigger;
import com.teammoeg.frostedheart.content.scenario.runner.trigger.VariantTrigger;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.chorda.util.CUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public class MCCommands {
	public void giveItem(ScenarioCommandContext runner, @Param("i") String item, @Param("n") String nbt, @Param("c") int count) throws CommandSyntaxException {
		Item i = CRegistryHelper.getItem(new ResourceLocation(item));
		if (count == 0) count = 1;
		ItemStack is = new ItemStack(i, count);
		if (nbt != null)
			is.setTag(TagParser.parseTag(nbt));
		CUtils.giveItem(runner.context().player(), is);
	}

	public void setResearchAttribute(ScenarioCommandContext runner, @Param("k") String key, @Param("v") double value) {
		ResearchDataAPI.putVariantDouble(runner.context().player(), key, value);
	}

	public void waitPlayerStart(ScenarioCommandContext runner, @Param("s") String s, @Param("l") String l) {
		runner.thread().addTrigger(new OrTrigger(new MovementTrigger(runner.context().player()), new VariantTrigger().register(runner.context().player(), EventTriggerType.PLAYER_INTERACT)).setSync(),
			new ExecuteTarget( s, l));
	}

	public void gameCommand(ScenarioCommandContext runner,@Param("op")boolean op,@Param("asPlayer")boolean asp, @Param("cmd") @Param("command") String s) {
		Map<String, Object> overrides = new HashMap<>();
		ServerPlayer triggerPlayer = (ServerPlayer) runner.context().player();
		overrides.put("p", triggerPlayer.getGameProfile().getName());

		BlockPos pos = triggerPlayer.blockPosition();
		overrides.put("x", pos.getX());
		overrides.put("y", pos.getY());
		overrides.put("z", pos.getZ());
		ServerOpListEntry opent= CTeamDataManager.getServer().getPlayerList().getOps().get(triggerPlayer.getGameProfile());
		if(op)
			if(opent==null){
				CTeamDataManager.getServer().getPlayerList().op(triggerPlayer.getGameProfile());
			}
		try {
			Commands cmds = CTeamDataManager.getServer().getCommands();
			CommandSourceStack source = asp?triggerPlayer.createCommandSourceStack(): CTeamDataManager.getServer().createCommandSourceStack();
			for (Map.Entry<String, Object> entry : overrides.entrySet()) {
				if (entry.getValue() != null) {
					s = s.replace("@" + entry.getKey(), entry.getValue().toString());
				}
			}
			cmds.performPrefixedCommand(source, s);
		}finally {
			if(op)
				if(opent==null){
					CTeamDataManager.getServer().getPlayerList().deop(triggerPlayer.getGameProfile());
				}
		}
	}
}
