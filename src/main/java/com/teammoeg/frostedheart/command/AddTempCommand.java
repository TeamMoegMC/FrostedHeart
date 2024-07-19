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

import java.util.Collection;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.IHeatArea;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;

public class AddTempCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> add = Commands.literal("set")
                .then(Commands.argument("position", BlockPosArgument.blockPos()).executes((ct) -> {
                    ChunkHeatData.removeTempAdjust(ct.getSource().getWorld(), BlockPosArgument.getBlockPos(ct, "position"));
                    return Command.SINGLE_SUCCESS;
                }).then(Commands.argument("range", IntegerArgumentType.integer())
                        .then(Commands.argument("temperature", IntegerArgumentType.integer()).executes((ct) -> {
                            ChunkHeatData.addCubicTempAdjust(ct.getSource().getWorld(),
                                    BlockPosArgument.getBlockPos(ct, "position"),
                                    IntegerArgumentType.getInteger(ct, "range"),
                                    IntegerArgumentType.getInteger(ct, "temperature"));
                            return Command.SINGLE_SUCCESS;
                        }))));
        LiteralArgumentBuilder<CommandSource> get = Commands.literal("get")
                .executes((ct) -> {
                    Collection<IHeatArea> adjs = ChunkHeatData.getAdjust(ct.getSource().getWorld(), ct.getSource().asPlayer().getPosition());
                    if (adjs.isEmpty()) {
                        ct.getSource().sendFeedback(TranslateUtils.str("No Active Adjust!"), true);
                    } else {
                        ct.getSource().sendFeedback(TranslateUtils.str("Active Adjusts:"), true);
                        for (IHeatArea adj : adjs) {
                            ct.getSource().sendFeedback(TranslateUtils.str("center:" + adj.getCenter() + ",radius:" + adj.getRadius() + ",temperature:" + adj.getValueAt(ct.getSource().asPlayer().getPosition())), true);
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("position", BlockPosArgument.blockPos())
                        .executes((ct) -> {
                            Collection<IHeatArea> adjs = ChunkHeatData.getAdjust(ct.getSource().getWorld(), BlockPosArgument.getBlockPos(ct, "position"));
                            if (adjs.isEmpty()) {
                                ct.getSource().sendFeedback(TranslateUtils.str("No Active Adjust!"), true);
                            } else {
                                ct.getSource().sendFeedback(TranslateUtils.str("Active Adjusts:"), true);
                                for (IHeatArea adj : adjs) {
                                    ct.getSource().sendFeedback(TranslateUtils.str("center:" + adj.getCenter() + ",radius:" + adj.getRadius() + ",temperature:" + adj.getValueAt(BlockPosArgument.getBlockPos(ct, "position"))), true);
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        }));
        dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermissionLevel(2)).then(Commands.literal("temperature").then(add).then(get)));
    }
}
