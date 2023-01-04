/*
 * Copyright (c) 2022 TeamMoeg
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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.ClimateData;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

public class ClimateCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> get = Commands.literal("get")
                .executes((ct) -> {
                	try {
                           ct.getSource().sendFeedback(new StringTextComponent(String.valueOf(ClimateData.get(ct.getSource().getWorld()))),true);
                	}catch(Exception ex) {
                		ex.printStackTrace();
                	}
                            return Command.SINGLE_SUCCESS;
                        });
        LiteralArgumentBuilder<CommandSource> rebuild = Commands.literal("rebuild")
                .executes((ct) -> {
                	
                    ClimateData.get(ct.getSource().getWorld()).resetTempEvent(ct.getSource().getWorld());
                    ct.getSource().sendFeedback(new StringTextComponent("Succeed!").mergeStyle(TextFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
        });
        LiteralArgumentBuilder<CommandSource> init = Commands.literal("init")
        .executes((ct) -> {
            ClimateData.get(ct.getSource().getWorld()).addInitTempEvent(ct.getSource().getWorld());
            ct.getSource().sendFeedback(new StringTextComponent("Succeed!").mergeStyle(TextFormatting.GREEN), false);
            return Command.SINGLE_SUCCESS;
        });
        LiteralArgumentBuilder<CommandSource> reset = Commands.literal("resetVanilla")
                .executes((ct) -> {
                	ct.getSource().getWorld().serverWorldInfo.setThunderTime(0);
                	ct.getSource().getWorld().serverWorldInfo.setRainTime(0);
                	ct.getSource().getWorld().serverWorldInfo.setClearWeatherTime(0);
                    return Command.SINGLE_SUCCESS;
                });
        
        dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermissionLevel(2)).then(Commands.literal("climate").then(get).then(init).then(rebuild).then(reset)));
    }
}
