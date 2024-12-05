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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.util.lang.Lang;
import com.teammoeg.frostedheart.util.constants.FHTemperatureDifficulty;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;

public class TemperatureCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // Use capability PlayerTemperatureData

        // Get previousTemp, bodyTemp, envTemp, feelTemp, difficulty
        LiteralArgumentBuilder<CommandSourceStack> get = Commands.literal("get").executes((ct) -> {
                    Player player = ct.getSource().getPlayerOrException();
                    PlayerTemperatureData.getCapability(player).ifPresent(data -> {
                        ct.getSource().sendSuccess(()-> Lang.str("Body Temperature: " + data.getBodyTemp()), true);
                        ct.getSource().sendSuccess(()-> Lang.str("Environment Temperature: " + data.getEnvTemp()), true);
                        ct.getSource().sendSuccess(()-> Lang.str("Feel Temperature: " + data.getFeelTemp()), true);
                        ct.getSource().sendSuccess(()-> Lang.str("Previous Temperature: " + data.getPreviousTemp()), true);
                        ct.getSource().sendSuccess(()-> Lang.str("Self-Heating Difficulty: " + data.getDifficulty().name()), true);
                    });
                    return Command.SINGLE_SUCCESS;
                });

        // Set difficulty
        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set")
                .then(Commands.literal("difficulty")
                        .then(Commands.literal("easy").executes((ct) -> {
                            Player player = ct.getSource().getPlayerOrException();
                            PlayerTemperatureData.getCapability(player).ifPresent(data -> {
                                data.setDifficulty(FHTemperatureDifficulty.easy);
                            });
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("normal").executes((ct) -> {
                            Player player = ct.getSource().getPlayerOrException();
                            PlayerTemperatureData.getCapability(player).ifPresent(data -> {
                                data.setDifficulty(FHTemperatureDifficulty.normal);
                            });
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("hard").executes((ct) -> {
                            Player player = ct.getSource().getPlayerOrException();
                            PlayerTemperatureData.getCapability(player).ifPresent(data -> {
                                data.setDifficulty(FHTemperatureDifficulty.hard);
                            });
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("hardcore").executes((ct) -> {
                            Player player = ct.getSource().getPlayerOrException();
                            PlayerTemperatureData.getCapability(player).ifPresent(data -> {
                                data.setDifficulty(FHTemperatureDifficulty.hardcore);
                            });
                            return Command.SINGLE_SUCCESS;
                        }))
                );


        for (String string : new String[]{FHMain.MODID, FHMain.ALIAS, FHMain.TWRID}) {
            dispatcher.register(Commands.literal(string).requires(s -> s.hasPermission(2))
                    .then(Commands.literal("temperature")
                            .then(get)
                            .then(set)
                    )
            );
        }
    }
}
