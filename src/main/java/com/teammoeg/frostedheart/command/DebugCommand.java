/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.command;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.relic.RelicData;
import com.teammoeg.frostedheart.util.FileUtil;
import com.teammoeg.frostedheart.world.FHDimensions;
import com.teammoeg.frostedheart.world.FHFeatures;

import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.task.CheckmarkTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.loading.FMLPaths;

public class DebugCommand {

	public static BlockPos getRelicGridPos(ServerPlayerEntity player) {
		UUID teamID = FTBTeamsAPI.getPlayerTeam(player).getId();
		return RelicData.get(player.world).getRelicPos(teamID);
	}

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> add = Commands.literal("debug")
				.then(Commands.literal("generate_relic").executes(ct -> {
					ServerWorld relicWorld = ct.getSource().getServer().getWorld(FHDimensions.RELIC_DIM);
					if (relicWorld == null) {
						ct.getSource().asPlayer().sendStatusMessage(new StringTextComponent("Relic World not loaded."), false);
					} else {
						BlockPos pos = getRelicGridPos(ct.getSource().asPlayer());
						FHFeatures.spacecraft_feature.generate(relicWorld, relicWorld.getChunkProvider().getChunkGenerator(), relicWorld.rand, pos);
						ct.getSource().asPlayer().sendStatusMessage(new StringTextComponent("Generated relic structure at " + pos), false);
					}
					return Command.SINGLE_SUCCESS;
				}))
                .then(Commands.literal("generate_airship").executes(ct -> {
                    FHFeatures.spacecraft_feature.generate(((ServerWorld) ct.getSource().asPlayer().world), ((ServerWorld) ct.getSource().asPlayer().world).getChunkProvider().getChunkGenerator(), ct.getSource().asPlayer().world.rand,
                            ct.getSource().asPlayer().getPosition());
                    return Command.SINGLE_SUCCESS;
                })).then(Commands.literal("export_quests").executes(ct -> {
                	List<Quest> quests=FTBQuests.PROXY.getQuestFile(false).chapterGroups.stream().flatMap(e->e.chapters.stream()).flatMap(e->e.quests.stream())
                     .collect(Collectors.toList());
                	JsonArray ja=new JsonArray();
                	Gson gs=new GsonBuilder().setPrettyPrinting().create();
                	quests.stream().map(e->{
                		JsonObject jo=new JsonObject();
                		jo.addProperty("title", e.getTitle().getString());
                		jo.addProperty("subtitle", e.getSubtitle().getString());
                		jo.addProperty("chapter", e.getQuestChapter().getTitle().getString());
                		JsonArray dec=new JsonArray();
                		for(ITextComponent it:e.getDescription()) {
                			dec.add(it.getString());
                		}
                		jo.add("description", dec);
                		JsonArray fow=new JsonArray();
                		for(QuestObject qo:e.dependencies) {
                			fow.add(qo.getTitle().getString());
                		}
                		jo.add("parents", fow);
                		JsonArray chi=new JsonArray();
                		for(QuestObject qo:e.getDependants()) {
                			chi.add(qo.getTitle().getString());
                		}
                		jo.add("children",chi);
                		JsonArray tsk=new JsonArray();
                		
                		for(Task t:e.tasks) {
                			String out="";
                			if(t instanceof CheckmarkTask) {
                				out="√ "+t.getTitle().getString();
                			}else
                				out=t.getTitle().getString();
                			tsk.add(out);
                		}
                		jo.add("tasks", tsk);
                		JsonArray rwd=new JsonArray();
                		for(Reward r:e.rewards) {
                			rwd.add(r.getTitle().getString());
                		}
                		jo.add("rewards", rwd);
                		
                		return jo;
                	}).forEach(ja::add);
                	try {
						FileUtil.transfer(gs.toJson(ja), new File(FMLPaths.GAMEDIR.get().toFile(),"quest_export.json"));
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
                    return Command.SINGLE_SUCCESS;
                }));
        
        dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermissionLevel(2)).then(add));
    }
  
}
