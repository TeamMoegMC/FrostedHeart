/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.team.FHTeamDataManager;
import com.teammoeg.frostedheart.base.team.TeamDataClosure;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.util.Lang;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.ChatFormatting;

public class ResearchCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> add = Commands.literal("research")
                // add insight
                .then(Commands.literal("insight").then(Commands.literal("add").then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(ct -> {
                    TeamResearchData trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException()).get();
                    trd.addInsight(ct.getArgument("amount", Integer.class));
                    ct.getSource().sendSuccess(()-> Lang.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                })))
                        // get insight
                .then(Commands.literal("get").executes(ct -> {
                    TeamResearchData trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException()).get();
                    ct.getSource().sendSuccess(()-> Lang.str("Insight: " + trd.getInsight()).withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                }))
                        // Get insight level
                .then(Commands.literal("getLevel").executes(ct -> {
                    TeamResearchData trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException()).get();
                    ct.getSource().sendSuccess(()-> Lang.str("Insight Level: " + trd.getInsightLevel()).withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                }))
                        // Get used insight level
                .then(Commands.literal("getUsedLevel").executes(ct -> {
                    TeamResearchData trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException()).get();
                    ct.getSource().sendSuccess(()-> Lang.str("Used Insight Level: " + trd.getUsedInsightLevel()).withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                }))
                        // set insight
                .then(Commands.literal("set").then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(ct -> {
                    TeamResearchData trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException()).get();
                    trd.setInsight(ct.getArgument("amount", Integer.class));
                    ct.getSource().sendSuccess(()-> Lang.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                })))
                        // set insight level
                .then(Commands.literal("setLevel").then(Commands.argument("level", IntegerArgumentType.integer(0)).executes(ct -> {
                    TeamResearchData trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException()).get();
                    trd.setInsightLevel(ct.getArgument("level", Integer.class));
                    ct.getSource().sendSuccess(()-> Lang.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                })))
                        // set used insight level
                .then(Commands.literal("setUsedLevel").then(Commands.argument("level", IntegerArgumentType.integer(0)).executes(ct -> {
                    TeamResearchData trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException()).get();
                    trd.setUsedInsightLevel(ct.getArgument("level", Integer.class));
                    ct.getSource().sendSuccess(()-> Lang.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                })))

                )


                .then(Commands.literal("complete").then(Commands.argument("name", StringArgumentType.string()).suggests((ct, s) -> {
                    for (Research r : FHResearch.getAllResearch())
                        if (r.getId().startsWith(s.getRemaining()))
                            s.suggest(r.getId(), r.getName());
                    return s.buildFuture();
                }).executes(ct -> {
                    String rsn = ct.getArgument("name", String.class);

                    Research rs = FHResearch.getResearch(rsn);
                    if (rs == null) {
                        ct.getSource().sendFailure(Lang.str("Research not found").withStyle(ChatFormatting.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    TeamDataClosure<TeamResearchData> rd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException());
                    rd.get().setResearchFinished(rd.team(), rs, true);
                    ct.getSource().sendSuccess(()-> Lang.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                })).then(Commands.literal("all").executes(ct -> {
                    TeamDataClosure<TeamResearchData> trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException());
                    for (Research r : FHResearch.getAllResearch()) {
                        if (r.isInCompletable()) continue;
                        trd.get().setResearchFinished(trd.team(), r, true);
                    }
                    ct.getSource().sendSuccess(()-> Lang.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("transfer").then(Commands.argument("from", UuidArgument.uuid())
                        .then(Commands.argument("to", UuidArgument.uuid())).executes(ct -> {
                            Team team = FTBTeamsAPI.api().getManager().getTeamByID(UuidArgument.getUuid(ct, "to")).orElse(null);
                            FHTeamDataManager.INSTANCE.transfer(UuidArgument.getUuid(ct, "from"), team);
                            ct.getSource().sendSuccess(()-> Lang.str("Transfered to " + team.getName()).withStyle(ChatFormatting.GREEN), false);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("edit").then(Commands.argument("enable", BoolArgumentType.bool()).executes(ct -> {
                    FHResearch.editor = ct.getArgument("enable", Boolean.class);
                    ct.getSource().sendSuccess(()-> Lang.str("Editing mode set " + FHResearch.editor).withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("default").executes(ct -> Command.SINGLE_SUCCESS))
                .then(Commands.literal("energy").executes(ct -> {
                    EnergyCore.reportEnergy(ct.getSource().getPlayerOrException());
                    return Command.SINGLE_SUCCESS;
                }).then(Commands.literal("add").then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(ct -> {
                	
                    EnergyCore.addPoint(ct.getSource().getPlayerOrException(), ct.getArgument("amount", Integer.class));
                    return Command.SINGLE_SUCCESS;
                }))))

                .then(Commands.literal("attribute").then(Commands.argument("name", StringArgumentType.string()).suggests((ct, s) -> {
                            CompoundTag cnbt = ResearchDataAPI.getVariants(ct.getSource().getPlayerOrException());
                            cnbt.getAllKeys().forEach(s::suggest);
                            return s.buildFuture();

                        }).executes(ct -> {
                            CompoundTag cnbt = ResearchDataAPI.getVariants(ct.getSource().getPlayerOrException());
                            String rsn = ct.getArgument("name", String.class);
                            ct.getSource().sendSuccess(()-> Lang.str(String.valueOf(cnbt.get(rsn))), false);
                            return Command.SINGLE_SUCCESS;
                        })).then(Commands.literal("all").executes(ct -> {

                            CompoundTag cnbt = ResearchDataAPI.getVariants(ct.getSource().getPlayerOrException());
                            ct.getSource().sendSuccess(()-> Lang.str(cnbt.toString()), false);
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
                        })))))
                .then(Commands.literal("reset").then(Commands.argument("name", StringArgumentType.string()).suggests((ct, s) -> {
                    for (Research r : FHResearch.getAllResearch())
                        if (r.getId().startsWith(s.getRemaining()))
                            s.suggest(r.getId(), r.getName());
                    return s.buildFuture();
                }).executes(ct -> {
                    String rsn = ct.getArgument("name", String.class);
                    TeamDataClosure<TeamResearchData> trd=ResearchDataAPI.getData(ct.getSource().getPlayerOrException());
                    
                    trd.get().resetData(trd.team(),FHResearch.getResearch(rsn));
                    return Command.SINGLE_SUCCESS;
                })).then(Commands.literal("all").executes(ct -> {
                    TeamDataClosure<TeamResearchData> trd = ResearchDataAPI.getData(ct.getSource().getPlayerOrException());
                    for (Research r : FHResearch.getAllResearch()) {
                        trd.get().resetData(trd.team(),r);
                    }
                    return Command.SINGLE_SUCCESS;
                })));
        dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermission(2)).then(add));
    }
}
