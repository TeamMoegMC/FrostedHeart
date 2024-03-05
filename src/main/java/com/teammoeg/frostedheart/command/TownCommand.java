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

package com.teammoeg.frostedheart.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TownResourceType;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.util.client.GuiUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;

import java.util.Arrays;

public class TownCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {

        LiteralArgumentBuilder<CommandSource> name =
                Commands.literal("name")
                        .executes(ct -> {
                            TeamTown town = TeamTown.from(ct.getSource().asPlayer());
                            ct.getSource().sendFeedback(GuiUtils.str(town.getName()), true);
                            return Command.SINGLE_SUCCESS;
                        });

        LiteralArgumentBuilder<CommandSource> listResources =
                Commands.literal("list")
                        .executes(ct -> {
                            TeamTown town = TeamTown.from(ct.getSource().asPlayer());
                            ct.getSource().sendFeedback(GuiUtils.str(town.getResources()), true);
                            return Command.SINGLE_SUCCESS;
                        });

        LiteralArgumentBuilder<CommandSource> addResources =
                Commands.literal("add")
                        .then(Commands.argument("type", StringArgumentType.string())
                                .suggests((ct, s) -> {
                                    // Get all TownResourceType enum values
                                    Arrays.stream(TownResourceType.values()).forEach(t -> s.suggest(t.getKey()));
                                    return s.buildFuture();
                                })
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                        .executes(ct -> {
                                            double amount = DoubleArgumentType.getDouble(ct, "amount");
                                            String type = StringArgumentType.getString(ct, "type");
                                            TeamTown town = TeamTown.from(ct.getSource().asPlayer());
                                            town.add(TownResourceType.from(type), amount, false);
                                            ct.getSource().sendFeedback(GuiUtils.str("Resource added"), true);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        );

        LiteralArgumentBuilder<CommandSource> listResidents =
                Commands.literal("list").executes(ct -> {
                    TeamTown town = TeamTown.from(ct.getSource().asPlayer());
                            int size = town.getResidents().values().size();
                            ct.getSource().sendFeedback(GuiUtils.str("Total residents: " + size), true);
                            ct.getSource().sendFeedback(GuiUtils.str(town.getResidents().values()), true);
                            return Command.SINGLE_SUCCESS;
                        });

        LiteralArgumentBuilder<CommandSource> addResident =
                Commands.literal("add")
                        .then(Commands.argument("first_name", StringArgumentType.string())
                                .then(Commands.argument("last_name", StringArgumentType.string()).executes(ct -> {
                                    TeamTown town = TeamTown.from(ct.getSource().asPlayer());
                                    town.addResident(new Resident(StringArgumentType.getString(ct, "first_name"), StringArgumentType.getString(ct, "last_name")));
                                    ct.getSource().sendFeedback(GuiUtils.str("Resident added"), true);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        );

        LiteralArgumentBuilder<CommandSource> listBlocks =
                Commands.literal("list").executes(ct -> {
                    TeamTown town = TeamTown.from(ct.getSource().asPlayer());
                    ct.getSource().sendFeedback(GuiUtils.str("Total blocks: " + town.getTownBlocks().size()), true);
                    town.getTownBlocks().forEach((k, v) -> {
                        String blockName = v.getType().getBlock().getTranslationKey();
                        ct.getSource().sendFeedback(GuiUtils.translate(blockName).appendSibling(GuiUtils.str(" at " + k)), true);
                    });
                    return Command.SINGLE_SUCCESS;
                });

        dispatcher.register(Commands.literal(FHMain.MODID)
                .requires(s -> s.hasPermissionLevel(2))
                .then(Commands.literal("town")
                        .then(name)
                        .then(Commands.literal("resources")
                                .then(listResources)
                                .then(addResources)
                        )
                        .then(Commands.literal("residents")
                                .then(listResidents)
                                .then(addResident)
                        )
                        .then(Commands.literal("blocks")
                                .then(listBlocks)
                        )
                )
        );
    }
}
