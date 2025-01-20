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
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.chorda.util.lang.Components;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.climate.FHTemperatureDifficulty;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TemperatureCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // Use capability PlayerTemperatureData

        // Get previousTemp, bodyTemp, envTemp, feelTemp, difficulty
        LiteralArgumentBuilder<CommandSourceStack> get = Commands.literal("get").executes((ct) -> {
                    Player player = ct.getSource().getPlayerOrException();
                    PlayerTemperatureData.getCapability(player).ifPresent(data -> {
                        ct.getSource().sendSuccess(()-> Components.str("Body Temperature: " + data.getBodyTemp()), true);
                        ct.getSource().sendSuccess(()-> Components.str("Environment Temperature: " + data.getEnvTemp()), true);
                        ct.getSource().sendSuccess(()-> Components.str("Feel Temperature: " + data.getFeelTemp()), true);
                        ct.getSource().sendSuccess(()-> Components.str("Previous Temperature: " + data.getPreviousTemp()), true);
                        ct.getSource().sendSuccess(()-> Components.str("Self-Heating Difficulty: " + data.getDifficulty().name()), true);
                    });
                    return Command.SINGLE_SUCCESS;
                });

        // Set difficulty
        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set")
                // Set difficulty
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
                )
                // Set bodyTemp
                .then(Commands.literal("bodyTemp")
                        .then(Commands.argument("value", FloatArgumentType.floatArg()).executes((ct) -> {
                            Player player = ct.getSource().getPlayerOrException();
                            PlayerTemperatureData.getCapability(player).ifPresent(data -> {
                                data.setBodyTemp(ct.getArgument("amount", Float.class));
                            });
                            return Command.SINGLE_SUCCESS;
                        }))
                )
                // Set envTemp
                .then(Commands.literal("envTemp")
                        .then(Commands.argument("value", FloatArgumentType.floatArg()).executes((ct) -> {
                            Player player = ct.getSource().getPlayerOrException();
                            PlayerTemperatureData.getCapability(player).ifPresent(data -> {
                                data.setEnvTemp(ct.getArgument("amount", Float.class));
                            });
                            return Command.SINGLE_SUCCESS;
                        }))
                )
                // Set feelTemp
                .then(Commands.literal("feelTemp")
                        .then(Commands.argument("value", FloatArgumentType.floatArg()).executes((ct) -> {
                            Player player = ct.getSource().getPlayerOrException();
                            PlayerTemperatureData.getCapability(player).ifPresent(data -> {
                                data.setFeelTemp(ct.getArgument("amount", Float.class));
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

        dispatcher.register(Commands.literal("temperature")
                .requires(s -> s.hasPermission(2))
                .then(get)
                .then(set)
        );
    }
}
