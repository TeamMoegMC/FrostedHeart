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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.team.SpecialDataManager;
import com.teammoeg.frostedheart.team.SpecialDataTypes;
import com.teammoeg.frostedheart.town.TeamTown;
import com.teammoeg.frostedheart.town.TeamTownData;
import com.teammoeg.frostedheart.town.TownResourceType;
import com.teammoeg.frostedheart.town.resident.Resident;
import com.teammoeg.frostedheart.util.client.GuiUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;

public class TownCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {

        LiteralArgumentBuilder<CommandSource> add = Commands.literal("town")
            .then(Commands.literal("name").executes(ct -> {
                TeamTownData data = SpecialDataManager.get(ct.getSource().asPlayer()).getData(SpecialDataTypes.TOWN_DATA);
                TeamTown town = new TeamTown(data);
                ct.getSource().sendFeedback(GuiUtils.str(data.getName()), true);
                return Command.SINGLE_SUCCESS;
            })).then(Commands.literal("resources").then(Commands.literal("prep_food")).executes(ct -> {
                TeamTownData data = SpecialDataManager.get(ct.getSource().asPlayer()).getData(SpecialDataTypes.TOWN_DATA);
                TeamTown town = new TeamTown(data);
                ct.getSource().sendFeedback(GuiUtils.str(String.valueOf(town.get(TownResourceType.PREP_FOOD))), true);
                return Command.SINGLE_SUCCESS;
            }).then(Commands.argument("add", IntegerArgumentType.integer()).executes(ct -> {
                TeamTownData data = SpecialDataManager.get(ct.getSource().asPlayer()).getData(SpecialDataTypes.TOWN_DATA);
                TeamTown town = new TeamTown(data);
                town.add(TownResourceType.PREP_FOOD, 100, false);
                return Command.SINGLE_SUCCESS;
            })).then(Commands.literal("residents").then(Commands.literal("list")).executes(ct -> {
                TeamTownData data = SpecialDataManager.get(ct.getSource().asPlayer()).getData(SpecialDataTypes.TOWN_DATA);
                TeamTown town = new TeamTown(data);
                ct.getSource().sendFeedback(GuiUtils.str(data.getResidents().values().toString()), true);
                return Command.SINGLE_SUCCESS;
            }).then(Commands.argument("add", IntegerArgumentType.integer()).executes(ct -> {
                TeamTownData data = SpecialDataManager.get(ct.getSource().asPlayer()).getData(SpecialDataTypes.TOWN_DATA);
                TeamTown town = new TeamTown(data);
                data.addResident(new Resident("Duck", "Egg"));
                return Command.SINGLE_SUCCESS;
            }))));
    }
}
