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
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.network.DisplayCustomTipPacket;
import com.teammoeg.frostedheart.content.tips.network.DisplayTipPacket;
import com.teammoeg.frostedheart.util.lang.Lang;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class TipCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var run = Commands.literal("tip").then(
            Commands.literal("add").then(
                Commands.argument("targets", EntityArgument.players()).then(
                Commands.argument("id", StringArgumentType.string())
                    .executes((c) -> {
                        String ID = c.getArgument("id", String.class);
                        int i = 0;

                        for(ServerPlayer sp : EntityArgument.getPlayers(c, "targets")) {
                            FHNetwork.send(PacketDistributor.PLAYER.with(() -> sp), new DisplayTipPacket(ID));
                            i++;
                        }

                        return i;
                    }
                )))).then(
            Commands.literal("add_custom").then(
                Commands.argument("targets", EntityArgument.players()).then(
                Commands.argument("title", StringArgumentType.string()).then(
                Commands.argument("content", StringArgumentType.string()).then(
                Commands.argument("display_time", IntegerArgumentType.integer())
                    .executes((c) -> {
                        String title = c.getArgument("title", String.class);
                        String content = c.getArgument("content", String.class);
                        Integer displayTime = c.getArgument("display_time", Integer.class);

                        int i = 0;
                        for(ServerPlayer sp : EntityArgument.getPlayers(c, "targets")) {
                            FHNetwork.send(PacketDistributor.PLAYER.with(() -> sp), new DisplayCustomTipPacket(toTip(title, content, displayTime)));
                            i++;
                        }

                        return i;
                    }
        ))))));

        dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermission(2)).then(run));
    }

    public static Tip toTip(String title, String content, int displayTime) {
        List<Component> contents = new ArrayList<>();
        if (!content.isEmpty()) {
            for (String s : content.split("\\$\\$")) {
                contents.add(I18n.exists(s) ? Lang.translateKey(s) : Lang.str(s));
            }
        }
        return Tip.builder(title)
                .line(Lang.str(title))
                .lines(contents)
                .displayTime(displayTime)
                .alwaysVisible(displayTime <= -1)
                .setTemporary()
                .build();
    }
}
