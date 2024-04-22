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

package com.teammoeg.frostedheart.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHTeamDataManager;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.content.research.data.ResearchData;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.util.TranslateUtils;

import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.NBTTagArgument;
import net.minecraft.command.arguments.UUIDArgument;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.text.TextFormatting;

public class ResearchCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> add = Commands.literal("research")
                .then(Commands.literal("complete").then(Commands.argument("name", StringArgumentType.string()).suggests((ct, s) -> {
                    for (Research r : FHResearch.getAllResearch())
                        if (r.getId().startsWith(s.getRemaining()))
                            s.suggest(r.getId(), r.getName());
                    return s.buildFuture();
                }).executes(ct -> {
                    String rsn = ct.getArgument("name", String.class);

                    Research rs = FHResearch.getResearch(rsn).get();
                    if (rs == null) {
                        ct.getSource().sendErrorMessage(TranslateUtils.str("Research not found").mergeStyle(TextFormatting.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    ResearchData rd = ResearchDataAPI.getData(ct.getSource().asPlayer()).getData(rs);
                    rd.setFinished(true);
                    rd.announceCompletion();

                    ct.getSource().sendFeedback(TranslateUtils.str("Succeed!").mergeStyle(TextFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                })).then(Commands.literal("all").executes(ct -> {
                    TeamResearchData trd = ResearchDataAPI.getData(ct.getSource().asPlayer());
                    for (Research r : FHResearch.getAllResearch()) {
                        if (r.isInCompletable()) continue;
                        ResearchData rd = trd.getData(r);
                        rd.setFinished(true);
                        rd.announceCompletion();
                    }
                    ct.getSource().sendFeedback(TranslateUtils.str("Succeed!").mergeStyle(TextFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("transfer").then(Commands.argument("from", UUIDArgument.func_239194_a_())
                        .then(Commands.argument("to", UUIDArgument.func_239194_a_())).executes(ct -> {
                            Team team = FTBTeamsAPI.getManager().getTeamByID(UUIDArgument.func_239195_a_(ct, "to"));
                            FHTeamDataManager.INSTANCE.transfer(UUIDArgument.func_239195_a_(ct, "from"), team);
                            ct.getSource().sendFeedback(TranslateUtils.str("Transfered to " + team.getDisplayName()).mergeStyle(TextFormatting.GREEN), false);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("edit").then(Commands.argument("enable", BoolArgumentType.bool()).executes(ct -> {
                    FHResearch.editor = ct.getArgument("enable", Boolean.class);
                    ct.getSource().sendFeedback(TranslateUtils.str("Editing mode set " + FHResearch.editor).mergeStyle(TextFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("default").executes(ct -> Command.SINGLE_SUCCESS))
                .then(Commands.literal("energy").executes(ct -> {
                    EnergyCore.reportEnergy(ct.getSource().asPlayer());
                    return Command.SINGLE_SUCCESS;
                }).then(Commands.literal("add").then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(ct -> {
                	
                    EnergyCore.addPoint(ct.getSource().asPlayer(), ct.getArgument("amount", Integer.class));
                    return Command.SINGLE_SUCCESS;
                }))))

                .then(Commands.literal("attribute").then(Commands.argument("name", StringArgumentType.string()).suggests((ct, s) -> {
                            CompoundNBT cnbt = ResearchDataAPI.getVariants(ct.getSource().asPlayer());
                            cnbt.keySet().forEach(s::suggest);
                            return s.buildFuture();

                        }).executes(ct -> {
                            CompoundNBT cnbt = ResearchDataAPI.getVariants(ct.getSource().asPlayer());
                            String rsn = ct.getArgument("name", String.class);
                            ct.getSource().sendFeedback(TranslateUtils.str(String.valueOf(cnbt.get(rsn))), false);
                            return Command.SINGLE_SUCCESS;
                        })).then(Commands.literal("all").executes(ct -> {

                            CompoundNBT cnbt = ResearchDataAPI.getVariants(ct.getSource().asPlayer());
                            ct.getSource().sendFeedback(TranslateUtils.str(cnbt.toString()), false);
                            return Command.SINGLE_SUCCESS;

                        }))

                        .then(Commands.literal("set").then(Commands.argument("name", StringArgumentType.string()).suggests((ct, s) -> {
                            CompoundNBT cnbt = ResearchDataAPI.getVariants(ct.getSource().asPlayer());
                            cnbt.keySet().forEach(s::suggest);

                            if ("all".startsWith(s.getRemaining()))
                                s.suggest("all");
                            return s.buildFuture();

                        }).then(Commands.argument("value", NBTTagArgument.func_218085_a()).executes(ct -> {
                            CompoundNBT cnbt = ResearchDataAPI.getVariants(ct.getSource().asPlayer());
                            String rsn = ct.getArgument("name", String.class);
                            INBT nbt = ct.getArgument("value", INBT.class);
                            cnbt.put(rsn, nbt);
                            ResearchDataAPI.sendVariants(ct.getSource().asPlayer());
                            return Command.SINGLE_SUCCESS;
                        })))))
                .then(Commands.literal("reset").then(Commands.argument("name", StringArgumentType.string()).suggests((ct, s) -> {
                    for (Research r : FHResearch.getAllResearch())
                        if (r.getId().startsWith(s.getRemaining()))
                            s.suggest(r.getId(), r.getName());
                    return s.buildFuture();
                }).executes(ct -> {
                    String rsn = ct.getArgument("name", String.class);
                    ResearchDataAPI.getData(ct.getSource().asPlayer()).resetData(FHResearch.getResearch(rsn).get(), true);
                    return Command.SINGLE_SUCCESS;
                })).then(Commands.literal("all").executes(ct -> {
                    TeamResearchData trd = ResearchDataAPI.getData(ct.getSource().asPlayer());
                    for (Research r : FHResearch.getAllResearch()) {
                        trd.resetData(r, true);
                    }
                    return Command.SINGLE_SUCCESS;
                })));
        dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermissionLevel(2)).then(add));
    }
}
