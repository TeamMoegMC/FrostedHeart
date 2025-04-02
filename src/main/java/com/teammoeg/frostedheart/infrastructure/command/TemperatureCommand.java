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

package com.teammoeg.frostedheart.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.climate.FHTemperatureDifficulty;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TemperatureCommand {
    // Helper method to get color codes based on temperature (optional)
    private static String getTemperatureColorCode(float temp) {
        if (temp >= 39.0f) return "§c"; // Red for hot
        if (temp >= 37.0f) return "§e"; // Yellow for warm
        if (temp <= 34.0f) return "§9"; // Blue for cold
        if (temp <= 35.5f) return "§b"; // Light blue for cool
        return "§a"; // Green for normal
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // Use capability PlayerTemperatureData

        // Get previousTemp, bodyTemp, envTemp, feelTemp, difficulty
        LiteralArgumentBuilder<CommandSourceStack> get = Commands.literal("get").executes((ct) -> {
            Player player = ct.getSource().getPlayerOrException();
            PlayerTemperatureData.getCapability(player).ifPresent(data -> {
                StringBuilder result = new StringBuilder();

                // Core information
                result.append("§e=== Player Temperature Data ===§r\n");
                result.append(String.format("§6Core:§r %.2f°C (Previous: %.2f°C)\n",
                        data.getCoreBodyTemp(), data.getPreviousCoreBodyTemp()));
                result.append(String.format("§6Feeling:§r %.1f°C\n", data.getTotalFeelTemp()));
                result.append(String.format("§6Environment:§r %.1f°C\n", data.getEnvTemp()));
                BlockPos pos = new BlockPos((int) player.getX(), (int) player.getEyeY(), (int) player.getZ());
                result.append(String.format("§6Air:§r %.1f°C | §6Soil:§r %.1f°C\n",
                        WorldTemperature.air(player.level(), pos),
                        WorldTemperature.block(player.level(), pos)
                ));
                result.append(String.format("§6Wind:§r %s | §6Openness:§r %.2f\n",
                        WorldTemperature.wind(player.level()),
                        data.getAirOpenness()
                ));

                // Body parts information
                result.append("\n§e=== Body Parts ===§r\n");

                // Display body part temperatures with proper formatting
                for (PlayerTemperatureData.BodyPart part : PlayerTemperatureData.BodyPart.values()) {
                    String partName = part.name().charAt(0) + part.name().substring(1).toLowerCase();
                    float body = data.getBodyTempByPart(part);
                    float felt = data.getFeelTempByPart(part);

                    // Color code based on temperature range (optional)
                    String colorCode = getTemperatureColorCode(body);
                    String colorCodeFelt = getTemperatureColorCode(felt);

                    result.append(String.format("§6%s Body:§r %s%.1f°C | §6Feeling: %s%.1f°C§r\n", partName, colorCode, body + 37, colorCodeFelt, felt));
                }

                // Send the compiled message as one comprehensive output
                ct.getSource().sendSuccess(() -> Components.str(result.toString()), false);
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
                                data.setAllPartsBodyTemp(ct.getArgument("amount", Float.class));
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
                                data.setAllPartsFeelTemp(ct.getArgument("amount", Float.class));
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
