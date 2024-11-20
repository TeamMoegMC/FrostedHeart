package com.teammoeg.frostedheart.content.scenario.commands;

import java.util.HashMap;
import java.util.Map;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.frostedheart.base.team.FHTeamDataManager;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.content.scenario.EventTriggerType;
import com.teammoeg.frostedheart.content.scenario.Param;
import com.teammoeg.frostedheart.content.scenario.runner.BaseScenarioRunner;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteTarget;
import com.teammoeg.frostedheart.content.scenario.runner.target.OrTrigger;
import com.teammoeg.frostedheart.content.scenario.runner.target.VariantTargetTrigger;
import com.teammoeg.frostedheart.content.scenario.runner.target.trigger.MovementTrigger;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.RegistryUtils;

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
	public void giveItem(BaseScenarioRunner runner, @Param("i") String item, @Param("n") String nbt, @Param("c") int count) throws CommandSyntaxException {
		Item i = RegistryUtils.getItem(new ResourceLocation(item));
		if (count == 0) count = 1;
		ItemStack is = new ItemStack(i, count);
		if (nbt != null)
			is.setTag(TagParser.parseTag(nbt));
		FHUtils.giveItem(runner.getPlayer(), is);
	}

	public void setResearchAttribute(BaseScenarioRunner runner, @Param("k") String key, @Param("v") double value) {
		ResearchDataAPI.putVariantDouble(runner.getPlayer(), key, value);
	}

	public void waitPlayerStart(BaseScenarioRunner runner, @Param("s") String s, @Param("l") String l) {
		runner.addTrigger(new OrTrigger(new MovementTrigger(runner.getPlayer()), new VariantTargetTrigger().register(runner.getPlayer(), EventTriggerType.PLAYER_INTERACT)).setSync(),
			new ExecuteTarget(runner, s, l));
	}

	public void gameCommand(BaseScenarioRunner runner,@Param("op")int op,@Param("asPlayer")int asp, @Param("cmd") @Param("command") String s) {
		Map<String, Object> overrides = new HashMap<>();
		ServerPlayer triggerPlayer = (ServerPlayer) runner.getPlayer();
		overrides.put("p", triggerPlayer.getGameProfile().getName());

		BlockPos pos = triggerPlayer.blockPosition();
		overrides.put("x", pos.getX());
		overrides.put("y", pos.getY());
		overrides.put("z", pos.getZ());
		ServerOpListEntry opent= FHTeamDataManager.getServer().getPlayerList().getOps().get(triggerPlayer.getGameProfile());
		if(op>0)
			if(opent==null){
				FHTeamDataManager.getServer().getPlayerList().op(triggerPlayer.getGameProfile());
			}
		Commands cmds = FHTeamDataManager.getServer().getCommands();
		CommandSourceStack source = asp>0?triggerPlayer.createCommandSourceStack(): FHTeamDataManager.getServer().createCommandSourceStack();
		for (Map.Entry<String, Object> entry : overrides.entrySet()) {
			if (entry.getValue() != null) {
				s = s.replace("@" + entry.getKey(), entry.getValue().toString());
			}
		}
		cmds.performPrefixedCommand(source, s);
		if(op>0)
			if(opent==null){
				FHTeamDataManager.getServer().getPlayerList().deop(triggerPlayer.getGameProfile());
			}
	}
}
