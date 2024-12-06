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
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;
import com.teammoeg.frostedheart.util.lang.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class NutritionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Define nutrients
        String[] nutrients = {"fat", "carbohydrate", "protein", "vegetable"};

        LiteralArgumentBuilder<CommandSourceStack> nutrition = Commands.literal("nutrition");

        for (String nutrient : nutrients) {
            LiteralArgumentBuilder<CommandSourceStack> nutrientCommand = Commands.literal(nutrient)
                    // Get Command
                    .then(Commands.literal("get").executes(ct -> {
                        NutritionCapability.getCapability(ct.getSource().getPlayer()).ifPresent(data -> {
                            float value = getNutrientValue(data, nutrient);
                            ct.getSource().sendSuccess(() -> Lang.str(capitalize(nutrient) + ": " + value).withStyle(ChatFormatting.GREEN), false);
                        });
                        return Command.SINGLE_SUCCESS;
                    }))
                    // Add Command
                    .then(Commands.literal("add")
                            .then(Commands.argument("amount", FloatArgumentType.floatArg()).executes(ct -> {
                                NutritionCapability.getCapability(ct.getSource().getPlayer()).ifPresent(data -> {
                                    float amount = ct.getArgument("amount", Float.class);
                                    addNutrientValue(data, nutrient, ct.getSource().getPlayer(), amount);
                                    ct.getSource().sendSuccess(() -> Lang.str("Added " + amount + " to " + capitalize(nutrient)).withStyle(ChatFormatting.GREEN), false);
                                });
                                return Command.SINGLE_SUCCESS;
                            }))
                    )
                    // Set Command
                    .then(Commands.literal("set")
                            .then(Commands.argument("amount", FloatArgumentType.floatArg()).executes(ct -> {
                                NutritionCapability.getCapability(ct.getSource().getPlayer()).ifPresent(data -> {
                                    float amount = ct.getArgument("amount", Float.class);
                                    setNutrientValue(data, nutrient, amount);
                                    ct.getSource().sendSuccess(() -> Lang.str("Set " + capitalize(nutrient) + " to " + amount).withStyle(ChatFormatting.GREEN), false);
                                });
                                return Command.SINGLE_SUCCESS;
                            }))
                    )
                    // Fill Command
                    .then(Commands.literal("fill").executes(ct -> {
                        NutritionCapability.getCapability(ct.getSource().getPlayer()).ifPresent(data -> {
                            setNutrientValue(data, nutrient, 10000.0f);
                            ct.getSource().sendSuccess(() -> Lang.str(capitalize(nutrient) + " filled to max").withStyle(ChatFormatting.GREEN), false);
                        });
                        return Command.SINGLE_SUCCESS;
                    }));

            nutrition.then(nutrientCommand);
        }

        dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermission(2)).then(nutrition));
    }

    private static float getNutrientValue(NutritionCapability data, String nutrient) {
        switch (nutrient) {
            case "fat":
                return data.getFat();
            case "carbohydrate":
                return data.getCarbohydrate();
            case "protein":
                return data.getProtein();
            case "vegetable":
                return data.getVegetable();
            default:
                return 0.0f;
        }
    }

    private static void addNutrientValue(NutritionCapability data, String nutrient, net.minecraft.world.entity.player.Player player, float amount) {
        switch (nutrient) {
            case "fat":
                data.addFat(player, amount);
                break;
            case "carbohydrate":
                data.addCarbohydrate(player, amount);
                break;
            case "protein":
                data.addProtein(player, amount);
                break;
            case "vegetable":
                data.addVegetable(player, amount);
                break;
        }
    }

    private static void setNutrientValue(NutritionCapability data, String nutrient, float amount) {
        switch (nutrient) {
            case "fat":
                data.setFat(amount);
                break;
            case "carbohydrate":
                data.setCarbohydrate(amount);
                break;
            case "protein":
                data.setProtein(amount);
                break;
            case "vegetable":
                data.setVegetable(amount);
                break;
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0,1).toUpperCase() + str.substring(1);
    }
}
