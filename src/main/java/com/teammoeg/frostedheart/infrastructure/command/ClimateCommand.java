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
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.ClimateEvent;
import com.teammoeg.frostedheart.content.climate.WorldClimate;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClimateCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> get = Commands.literal("get")
                .executes((ct) -> {
                    try {
                        ct.getSource().sendSuccess(()-> Components.str(String.valueOf(WorldClimate.get(ct.getSource().getLevel()))), true);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return Command.SINGLE_SUCCESS;
                });
        LiteralArgumentBuilder<CommandSourceStack> rebuild = Commands.literal("rebuild").then(Commands.literal("cache").executes(ct -> {

                    WorldClimate.get(ct.getSource().getLevel()).rebuildCache(ct.getSource().getLevel());
                    ct.getSource().sendSuccess(()-> Components.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .executes((ct) -> {

                    WorldClimate.get(ct.getSource().getLevel()).resetTempEvent(ct.getSource().getLevel());
                    ct.getSource().sendSuccess(()-> Components.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                });
        LiteralArgumentBuilder<CommandSourceStack> init = Commands.literal("init")
                .executes((ct) -> {
                    WorldClimate.get(ct.getSource().getLevel()).addInitTempEvent(ct.getSource().getLevel());
                    ct.getSource().sendSuccess(()-> Components.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                });

        LiteralArgumentBuilder<CommandSourceStack> app = Commands.literal("append").then(
                Commands.literal("warm").executes(ct -> {
                    WorldClimate.get(ct.getSource().getLevel()).appendTempEvent(ClimateEvent::getWarmClimateEvent);
                    ct.getSource().sendSuccess(()-> Components.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                })
        ).then(
                Commands.literal("cold").executes(ct -> {
                    WorldClimate.get(ct.getSource().getLevel()).appendTempEvent(ClimateEvent::getColdClimateEvent);
                    ct.getSource().sendSuccess(()-> Components.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                })
        ).then(
                Commands.literal("blizzard").executes(ct -> {
                    WorldClimate.get(ct.getSource().getLevel()).appendTempEvent(ClimateEvent::getBlizzardClimateEvent);
                    ct.getSource().sendSuccess(()-> Components.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                })
        ).executes(ct -> {
            WorldClimate.get(ct.getSource().getLevel()).appendTempEvent(ClimateEvent::getClimateEvent);
            ct.getSource().sendSuccess(()-> Components.str("Succeed!").withStyle(ChatFormatting.GREEN), false);
            return Command.SINGLE_SUCCESS;
        });
        LiteralArgumentBuilder<CommandSourceStack> reset = Commands.literal("resetVanilla")
                .executes((ct) -> {
                    ServerLevelData serverWorldInfo=ct.getSource().getLevel().serverLevelData;
                    serverWorldInfo.setThunderTime(0);
                    serverWorldInfo.setRainTime(0);
                    serverWorldInfo.setClearWeatherTime(0);
                    return Command.SINGLE_SUCCESS;
                });

        for (String string : new String[]{FHMain.MODID, FHMain.ALIAS, FHMain.TWRID}) {
            dispatcher.register(Commands.literal(string).requires(s -> s.hasPermission(2)).then(Commands.literal("climate").then(get).then(init).then(rebuild).then(reset).then(app)));
        }

        // register a simple /climate comand to skip /fh climate
        dispatcher.register(Commands.literal("climate").requires(s -> s.hasPermission(2)).then(get).then(init).then(rebuild).then(reset).then(app));
    }
}
