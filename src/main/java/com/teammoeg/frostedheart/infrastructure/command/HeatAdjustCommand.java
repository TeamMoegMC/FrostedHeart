/*
 * Copyright (c) 2026 TeamMoeg
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

import java.util.List;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.AnalyticCombineMode;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.AnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.AnalyticShape;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HeatAdjustCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        // Remove
        LiteralArgumentBuilder<CommandSourceStack> remove = Commands.literal("remove")
                .then(Commands.argument("position", BlockPosArgument.blockPos()).executes((ct) -> {
                    BlockPos position = BlockPosArgument.getBlockPos(ct, "position");
                    MinecraftThermalInput.removeGameplayAnalyticField(
                            ct.getSource().getLevel(), position.asLong());
                    return Command.SINGLE_SUCCESS;
                }));

        // Set
        LiteralArgumentBuilder<CommandSourceStack> add = Commands.literal("set")
                .then(Commands.argument("position", BlockPosArgument.blockPos()).executes((ct) -> {
                    BlockPos position = BlockPosArgument.getBlockPos(ct, "position");
                    MinecraftThermalInput.removeGameplayAnalyticField(
                            ct.getSource().getLevel(), position.asLong());
                    return Command.SINGLE_SUCCESS;
                }).then(Commands.argument("range", IntegerArgumentType.integer(1))
                        .then(Commands.argument("temperature", IntegerArgumentType.integer()).executes((ct) -> {
                            upsertField(ct, AnalyticShape.CUBE, 0, 0);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.literal("sphere").executes((ct) -> {
                            upsertField(ct, AnalyticShape.SPHERE, 0, 0);
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.argument("top", IntegerArgumentType.integer(0)).suggests((ct,sb)->sb.suggest(2).buildFuture())
								.then(Commands.argument("bottom", IntegerArgumentType.integer(0)).suggests((ct,sb)->sb.suggest(2).buildFuture())
                        		.executes(ct->{
                             upsertField(
                                     ct,
                                     AnalyticShape.PILLAR,
                                     IntegerArgumentType.getInteger(ct, "top"),
                                     IntegerArgumentType.getInteger(ct, "bottom"));
                             return Command.SINGLE_SUCCESS;
                        }))))));

        // Get
        LiteralArgumentBuilder<CommandSourceStack> get = Commands.literal("get")
                .executes((ct) -> {
                    BlockPos position = ct.getSource().getPlayerOrException().blockPosition();
                    List<AnalyticField> fields = MinecraftThermalInput.gameplayAnalyticFieldsAt(
                            ct.getSource().getLevel(), position);
                    if (fields.isEmpty()) {
                        ct.getSource().sendSuccess(()-> Components.str("No Active Adjust!"), true);
                    } else {
                        ct.getSource().sendSuccess(()-> Components.str("Active Adjusts:"), true);
                        for (AnalyticField field : fields) {
                            sendField(ct.getSource(), field);
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("position", BlockPosArgument.blockPos())
                        .executes((ct) -> {
                            BlockPos position = BlockPosArgument.getBlockPos(ct, "position");
                            List<AnalyticField> fields = MinecraftThermalInput.gameplayAnalyticFieldsAt(
                                    ct.getSource().getLevel(), position);
                            if (fields.isEmpty()) {
                                ct.getSource().sendSuccess(()-> Components.str("No Active Adjust!"), true);
                            } else {
                                ct.getSource().sendSuccess(()-> Components.str("Active Adjusts:"), true);
                                for (AnalyticField field : fields) {
                                    sendField(ct.getSource(), field);
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        }));

        for (String string : new String[]{FHMain.MODID, FHMain.ALIAS, FHMain.TWRID}) {
            dispatcher.register(Commands.literal(string).requires(s -> s.hasPermission(2)).then(Commands.literal("heat_adjust").then(add).then(get).then(remove)));
        }

        // simple alias to skip modid
        dispatcher.register(Commands.literal("heat_adjust").requires(s -> s.hasPermission(2)).then(add).then(get).then(remove));
    }

    private static void upsertField(
            CommandContext<CommandSourceStack> context,
            AnalyticShape shape,
            int upperExtent,
            int lowerExtent
    ) {
        CommandSourceStack source = context.getSource();
        BlockPos position = BlockPosArgument.getBlockPos(context, "position");
        int range = IntegerArgumentType.getInteger(context, "range");
        int temperature = IntegerArgumentType.getInteger(context, "temperature");
        MinecraftThermalInput.upsertGameplayAnalyticField(
                source.getLevel(),
                new AnalyticField(
                        position.asLong(),
                        0,
                        AnalyticCombineMode.OVERRIDE,
                        shape,
                        position.getX() + 0.5D,
                        position.getY() + 0.5D,
                        position.getZ() + 0.5D,
                        range,
                        upperExtent,
                        lowerExtent,
                        temperature));
    }

    private static void sendField(CommandSourceStack source, AnalyticField field) {
        BlockPos center = BlockPos.containing(
                field.centerX(), field.centerY(), field.centerZ());
        source.sendSuccess(
                () -> Components.str(
                        "center:" + center
                                + ",shape:" + field.shape().name().toLowerCase()
                                + ",radius:" + field.radius()
                                + ",temperature:" + field.temperatureC()),
                true);
    }
}
