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

import java.util.Arrays;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TownResourceType;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class TownCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        LiteralArgumentBuilder<CommandSourceStack> name =
                Commands.literal("name")
                        .executes(ct -> {
                            TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                            ct.getSource().sendSuccess(()->TranslateUtils.str(town.getName()), true);
                            return Command.SINGLE_SUCCESS;
                        });

        LiteralArgumentBuilder<CommandSourceStack> listResources =
                Commands.literal("list")
                        .executes(ct -> {
                            TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                            ct.getSource().sendSuccess(()->TranslateUtils.str(town.getResources()), true);
                            return Command.SINGLE_SUCCESS;
                        });

        LiteralArgumentBuilder<CommandSourceStack> addResources =
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
                                            TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                                            town.add(TownResourceType.from(type), amount, false);
                                            ct.getSource().sendSuccess(()->TranslateUtils.str("Resource added"), true);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        );

        LiteralArgumentBuilder<CommandSourceStack> listResidents =
                Commands.literal("list").executes(ct -> {
                    TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                            int size = town.getResidents().values().size();
                            ct.getSource().sendSuccess(()->TranslateUtils.str("Total residents: " + size), true);
                            ct.getSource().sendSuccess(()->TranslateUtils.str(town.getResidents().values()), true);
                            return Command.SINGLE_SUCCESS;
                        });

        LiteralArgumentBuilder<CommandSourceStack> addResident =
                Commands.literal("add")
                        .then(Commands.argument("first_name", StringArgumentType.string())
                                .then(Commands.argument("last_name", StringArgumentType.string()).executes(ct -> {
                                    TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                                    town.addResident(new Resident(StringArgumentType.getString(ct, "first_name"), StringArgumentType.getString(ct, "last_name")));
                                    ct.getSource().sendSuccess(()->TranslateUtils.str("Resident added"), true);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        );

        LiteralArgumentBuilder<CommandSourceStack> listBlocks =
                Commands.literal("list").executes(ct -> {
                    TeamTown town = TeamTown.from(ct.getSource().getPlayerOrException());
                    ct.getSource().sendSuccess(()->TranslateUtils.str("Total blocks: " + town.getTownBlocks().size()), true);
                    town.getTownBlocks().forEach((k, v) -> {
                        String blockName = v.getType().getBlock().getDescriptionId();
                        ct.getSource().sendSuccess(()->TranslateUtils.translate(blockName).append(TranslateUtils.str(" at " + k)), true);
                    });
                    return Command.SINGLE_SUCCESS;
                });

        dispatcher.register(Commands.literal(FHMain.MODID)
                .requires(s -> s.hasPermission(2))
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
