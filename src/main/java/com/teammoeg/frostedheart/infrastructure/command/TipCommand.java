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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.tips.network.DisplayCustomTipPacket;
import com.teammoeg.frostedheart.content.tips.network.DisplayTipPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TipCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> run = Commands.literal("tips").then(
            Commands.literal("add").then(
            Commands.argument("targets", EntityArgument.players()).then(
            Commands.argument("ID", StringArgumentType.string())
                .executes((c) -> {
                    String ID = c.getArgument("ID", String.class);
                    int i = 0;

                    for(ServerPlayer sp : EntityArgument.getPlayers(c, "targets")) {
                        FHNetwork.send(PacketDistributor.PLAYER.with(() -> sp), new DisplayTipPacket(ID));
                        i++;
                    }

                    return i;
                }
                ))).then(
                Commands.literal("custom").then(
                Commands.argument("targets", EntityArgument.players()).then(
                Commands.argument("title", StringArgumentType.string()).then(
                Commands.argument("content", StringArgumentType.string()).then(
                Commands.argument("visible_time", IntegerArgumentType.integer()).then(
                Commands.argument("history", BoolArgumentType.bool())
                    .executes((c) -> {
                        String title = c.getArgument("title", String.class);
                        String content = c.getArgument("content", String.class);
                        Integer visibleTime = c.getArgument("visible_time", Integer.class);
                        boolean history = c.getArgument("history", Boolean.class);

                        int i = 0;
                        for(ServerPlayer sp : EntityArgument.getPlayers(c, "targets")) {
                            FHNetwork.send(PacketDistributor.PLAYER.with(() -> sp), new DisplayCustomTipPacket(title, content, visibleTime, history));
                            i++;
                        }

                        return i;
                    }
        ))))))));
        for (String string : new String[]{FHMain.MODID, FHMain.ALIAS, FHMain.TWRID}) {
            dispatcher.register(Commands.literal(string).requires(s -> s.hasPermission(2)).then(run));
        }
    }
}
