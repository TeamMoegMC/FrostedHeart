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

package com.teammoeg.frostedresearch;

import java.util.UUID;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.chorda.dataholders.team.AbstractTeam;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataClosure;
import com.teammoeg.chorda.dataholders.team.TeamsAPI;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedresearch.api.ResearchDataAPI;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FRMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ResearchCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> add = Commands.literal("research").requires(s -> s.hasPermission(2))
                // Insight
                .then(Commands.literal("insight")
                        // add insight
                        .then(Commands.literal("add").then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(ct -> {
                            TeamDataClosure<TeamResearchData> trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException());
                            trd.get().addInsight(trd.team(),ct.getArgument("amount", Integer.class));
                            ct.getSource().sendSuccess(() -> Components.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                            return Command.SINGLE_SUCCESS;
                        })))
                        // get insight
                        .then(Commands.literal("get").executes(ct -> {
                            TeamResearchData trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException()).get();
                            ct.getSource().sendSuccess(() -> Components.str("Insight: " + trd.getInsight()).withStyle(ChatFormatting.GREEN), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                        // Get insight level
                        .then(Commands.literal("getLevel").executes(ct -> {
                            TeamResearchData trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException()).get();
                            ct.getSource().sendSuccess(() -> Components.str("Insight Level: " + trd.getInsightLevel()).withStyle(ChatFormatting.GREEN), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                        // Get used insight level
                        .then(Commands.literal("getUsedLevel").executes(ct -> {
                            TeamResearchData trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException()).get();
                            ct.getSource().sendSuccess(() -> Components.str("Used Insight Level: " + trd.getUsedInsightLevel()).withStyle(ChatFormatting.GREEN), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                        // set insight
                        .then(Commands.literal("set").then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(ct -> {
                        	TeamDataClosure<TeamResearchData> trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException());
                            trd.get().setInsight(trd.team(),ct.getArgument("amount", Integer.class));
                            ct.getSource().sendSuccess(() -> Components.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                            return Command.SINGLE_SUCCESS;
                        })))
                        // set insight level
                        .then(Commands.literal("setLevel").then(Commands.argument("level", IntegerArgumentType.integer(0)).executes(ct -> {
                        	TeamDataClosure<TeamResearchData> trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException());
                            trd.get().setInsightLevel(trd.team(),ct.getArgument("level", Integer.class));
                            ct.getSource().sendSuccess(() -> Components.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                            return Command.SINGLE_SUCCESS;
                        })))
                        // set used insight level
                        .then(Commands.literal("setUsedLevel").then(Commands.argument("level", IntegerArgumentType.integer(0)).executes(ct -> {
                        	TeamDataClosure<TeamResearchData> trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException());
                            trd.get().setUsedInsightLevel(trd.team(),ct.getArgument("level", Integer.class));
                            ct.getSource().sendSuccess(() -> Components.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                            return Command.SINGLE_SUCCESS;
                        })))
                )


                // Complete
                .then(Commands.literal("complete")
                        // by name
                        .then(Commands.argument("name", StringArgumentType.string()).suggests((ct, s) -> {
                            for (Research r : FHResearch.getAllResearch())
                                if (r.getId().startsWith(s.getRemaining()))
                                    s.suggest(r.getId(), r.getName());
                            return s.buildFuture();
                        }).executes(ct -> {
                            String rsn = ct.getArgument("name", String.class);

                            Research rs = FHResearch.getResearch(rsn);
                            if (rs == null) {
                                ct.getSource().sendFailure(Components.str("Research not found").withStyle(ChatFormatting.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            TeamDataClosure<TeamResearchData> rd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException());
                            rd.get().setResearchFinished(rd.team(), rs, true);
                            ct.getSource().sendSuccess(() -> Components.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                        // all
                        .then(Commands.literal("all").executes(ct -> {
                            TeamDataClosure<TeamResearchData> trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException());
                            try {
                                for (Research r : FHResearch.getAllResearch()) {
                                    if (r.isInCompletable()) continue;
                                    trd.get().setResearchFinished(trd.team(), r, true);
                                }
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                            ct.getSource().sendSuccess(() -> Components.str("Succeed!").withStyle(ChatFormatting.GREEN), false);

                            return Command.SINGLE_SUCCESS;
                        })))
                // Transfer
                .then(Commands.literal("transfer")
                        .then(Commands.argument("from", UuidArgument.uuid()).then(Commands.argument("to", UuidArgument.uuid()).executes(ct -> {
                            UUID team = UuidArgument.getUuid(ct, "to");
                            AbstractTeam uteam=TeamsAPI.getAPI().getTeamByUuid(team);
                            if(uteam!=null) {
                            	CTeamDataManager.INSTANCE.transfer(UuidArgument.getUuid(ct, "from"), uteam);
                            	ct.getSource().sendSuccess(() -> Components.str("Transfered to " + uteam.getName()).withStyle(ChatFormatting.GREEN), false);
                            }else {
                            	ct.getSource().sendFailure(Components.str("Team not exists").withStyle(ChatFormatting.RED));
                            	return 0;
                            }
                            return Command.SINGLE_SUCCESS;
                        }))))
                // Edit
                .then(Commands.literal("edit")
                        .then(Commands.argument("enable", BoolArgumentType.bool()).executes(ct -> {
                            FHResearch.editor = ct.getArgument("enable", Boolean.class);
                            ct.getSource().sendSuccess(() -> Components.str("Editing mode set " + FHResearch.editor).withStyle(ChatFormatting.GREEN), false);
                            return Command.SINGLE_SUCCESS;
                        })))
                // Default
                .then(Commands.literal("default").executes(ct -> Command.SINGLE_SUCCESS))
                // Attribute
                .then(Commands.literal("attribute")
                        .then(Commands.argument("name", StringArgumentType.string()).suggests((ct, s) -> {
                            CompoundTag cnbt = ResearchDataAPI.getVariants(ct.getSource().getPlayerOrException());
                            cnbt.getAllKeys().forEach(s::suggest);
                            return s.buildFuture();

                        }).executes(ct -> {
                            CompoundTag cnbt = ResearchDataAPI.getVariants(ct.getSource().getPlayerOrException());
                            String rsn = ct.getArgument("name", String.class);
                            ct.getSource().sendSuccess(() -> Components.str(String.valueOf(cnbt.get(rsn))), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("all").executes(ct -> {

                            CompoundTag cnbt = ResearchDataAPI.getVariants(ct.getSource().getPlayerOrException());
                            ct.getSource().sendSuccess(() -> Components.str(cnbt.toString()), false);
                            return Command.SINGLE_SUCCESS;

                        }))
                        .then(Commands.literal("set").then(Commands.argument("name", StringArgumentType.string()).suggests((ct, s) -> {
                            CompoundTag cnbt = ResearchDataAPI.getVariants(ct.getSource().getPlayerOrException());
                            cnbt.getAllKeys().forEach(s::suggest);

                            if ("all".startsWith(s.getRemaining()))
                                s.suggest("all");
                            return s.buildFuture();

                        }).then(Commands.argument("value", NbtTagArgument.nbtTag()).executes(ct -> {
                            CompoundTag cnbt = ResearchDataAPI.getVariants(ct.getSource().getPlayerOrException());
                            String rsn = ct.getArgument("name", String.class);
                            Tag nbt = ct.getArgument("value", Tag.class);
                            cnbt.put(rsn, nbt);
                            ResearchDataAPI.sendVariants(ct.getSource().getPlayerOrException());
                            return Command.SINGLE_SUCCESS;
                        }))))
                )
                // Reset
                .then(Commands.literal("reset")
                        // by name
                        .then(Commands.argument("name", StringArgumentType.string()).suggests((ct, s) -> {
                            for (Research r : FHResearch.getAllResearch())
                                if (r.getId().startsWith(s.getRemaining()))
                                    s.suggest(r.getId(), r.getName());
                            return s.buildFuture();
                        }).executes(ct -> {
                            String rsn = ct.getArgument("name", String.class);
                            TeamDataClosure<TeamResearchData> trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException());

                            trd.get().resetData(trd.team(), FHResearch.getResearch(rsn));
                            return Command.SINGLE_SUCCESS;
                        }))
                        // all
                        .then(Commands.literal("all").executes(ct -> {
                            try {
                                TeamDataClosure<TeamResearchData> trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException());
                                for (Research r : FHResearch.getAllResearch()) {
                                    trd.get().resetData(trd.team(), r);
                                }
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                )
                // Get Research Information:
                // keyword: "info"
                // then, get research information by name
                // keyword: "name"
                .then(Commands.literal("info")
                        .then(Commands.argument("name", StringArgumentType.string()).suggests((ct, s) -> {
                            for (Research r : FHResearch.getAllResearch())
                                if (r.getId().startsWith(s.getRemaining()))
                                    s.suggest(r.getId(), r.getName());
                            return s.buildFuture();
                        })
                                .executes(ct -> {
                            String rsn = ct.getArgument("name", String.class);
                            Research rs = FHResearch.getResearch(rsn);
                            if (rs == null) {
                                ct.getSource().sendFailure(Components.str("Research not found").withStyle(ChatFormatting.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            TeamDataClosure<TeamResearchData> trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException());
                            ct.getSource().sendSuccess(() -> rs.getName().copy().append(Components.str( ": " + trd.get().getData(rs))), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                )

                // Get more detailed sub-information by getters
                // keyword: "get"
                // then, get research information by name
                // keyword: "name"
                // then, get sub-information by getter
                // keyword: "field_name"
                .then(Commands.literal("get")
                        .then(Commands.argument("name", StringArgumentType.string()).suggests((ct, s) -> {
                            for (Research r : FHResearch.getAllResearch())
                                if (r.getId().startsWith(s.getRemaining()))
                                    s.suggest(r.getId(), r.getName());
                            return s.buildFuture();
                        })
                                .then(Commands.argument("field_name", StringArgumentType.string()).suggests((ct, s) -> {
                            Research rs = FHResearch.getResearch(ct.getArgument("name", String.class));
                            if (rs == null) return s.buildFuture();
                            for (String key : rs.getData().getFieldNames())
                                if (key.startsWith(s.getRemaining()))
                                    s.suggest(key);
                            return s.buildFuture();
                        })
                                .executes(ct -> {
                            String rsn = ct.getArgument("name", String.class);
                            String field = ct.getArgument("field_name", String.class);
                            Research rs = FHResearch.getResearch(rsn);
                            if (rs == null) {
                                ct.getSource().sendFailure(Components.str("Research not found").withStyle(ChatFormatting.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            TeamDataClosure<TeamResearchData> trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException());
                            ct.getSource().sendSuccess(() -> Components.str(rs.getName() + ": " + trd.get().getData(rs).getField(field)).withStyle(ChatFormatting.GREEN), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                ));


        // Register
     
            dispatcher.register(Commands.literal("frostedheart").then(add));
            dispatcher.register(add);
        
    }
}
