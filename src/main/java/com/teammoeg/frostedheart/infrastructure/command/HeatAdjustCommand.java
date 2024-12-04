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

import java.util.Collection;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.IHeatArea;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;

public class HeatAdjustCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // Remove
        LiteralArgumentBuilder<CommandSourceStack> remove = Commands.literal("remove")
                .then(Commands.argument("position", BlockPosArgument.blockPos()).executes((ct) -> {
                    ChunkHeatData.removeTempAdjust(ct.getSource().getLevel(), BlockPosArgument.getBlockPos(ct, "position"));
                    return Command.SINGLE_SUCCESS;
                }));

        // Set
        LiteralArgumentBuilder<CommandSourceStack> add = Commands.literal("set")
                .then(Commands.argument("position", BlockPosArgument.blockPos()).executes((ct) -> {
                    ChunkHeatData.removeTempAdjust(ct.getSource().getLevel(), BlockPosArgument.getBlockPos(ct, "position"));
                    return Command.SINGLE_SUCCESS;
                }).then(Commands.argument("range", IntegerArgumentType.integer())
                        .then(Commands.argument("temperature", IntegerArgumentType.integer()).executes((ct) -> {
                            ChunkHeatData.addCubicTempAdjust(ct.getSource().getLevel(),
                                    BlockPosArgument.getBlockPos(ct, "position"),
                                    IntegerArgumentType.getInteger(ct, "range"),
                                    IntegerArgumentType.getInteger(ct, "temperature"));
                            return Command.SINGLE_SUCCESS;
                        }))));

        // Get
        LiteralArgumentBuilder<CommandSourceStack> get = Commands.literal("get")
                .executes((ct) -> {
                    Collection<IHeatArea> adjs = ChunkHeatData.getAdjust(ct.getSource().getLevel(), ct.getSource().getPlayerOrException().blockPosition());
                    if (adjs.isEmpty()) {
                        ct.getSource().sendSuccess(()->TranslateUtils.str("No Active Adjust!"), true);
                    } else {
                        ct.getSource().sendSuccess(()->TranslateUtils.str("Active Adjusts:"), true);
                        BlockPos pos=new BlockPos((int)ct.getSource().getPosition().x,(int)ct.getSource().getPosition().y,(int)ct.getSource().getPosition().z);
                        for (IHeatArea adj : adjs) {
                            ct.getSource().sendSuccess(()->TranslateUtils.str("center:" + adj.getCenter() + ",radius:" + adj.getRadius() + ",temperature:" + adj.getValueAt(pos)), true);
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("position", BlockPosArgument.blockPos())
                        .executes((ct) -> {
                            Collection<IHeatArea> adjs = ChunkHeatData.getAdjust(ct.getSource().getLevel(), BlockPosArgument.getBlockPos(ct, "position"));
                            if (adjs.isEmpty()) {
                                ct.getSource().sendSuccess(()->TranslateUtils.str("No Active Adjust!"), true);
                            } else {
                                ct.getSource().sendSuccess(()->TranslateUtils.str("Active Adjusts:"), true);
                                for (IHeatArea adj : adjs) {
                                    ct.getSource().sendSuccess(()->TranslateUtils.str("center:" + adj.getCenter() + ",radius:" + adj.getRadius() + ",temperature:" + adj.getValueAt(BlockPosArgument.getBlockPos(ct, "position"))), true);
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        }));

        for (String string : new String[]{FHMain.MODID, FHMain.ALIAS, FHMain.TWRID}) {
            dispatcher.register(Commands.literal(string).requires(s -> s.hasPermission(2)).then(Commands.literal("heat_adjust").then(add).then(get).then(remove)));
        }
    }
}
