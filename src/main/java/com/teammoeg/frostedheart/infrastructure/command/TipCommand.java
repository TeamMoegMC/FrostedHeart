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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.tips.ServerTipHelper;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.teammoeg.frostedheart.infrastructure.command.CommandHelper.*;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TipCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        var server = literal("tip")
                .then(literal("display").then(players("players").then(string("id")
                        .executes(TipCommand::display))))
                .then(literal("displayCustom")
                        .then(players("players").then(literal("json").then(string("json")
                                .executes(TipCommand::displayJson)))
                        .then(string("title").then(string("content").then(integer("displayTime")
                                .executes(TipCommand::displayCustom))))));

        for (String string : new String[]{FHMain.MODID, FHMain.ALIAS, FHMain.TWRID}) {
            dispatcher.register(Commands.literal(string).requires(s -> s.hasPermission(2)).then(server));
        }
        dispatcher.register(server);
    }

    private static int display(CommandContext<CommandSourceStack> ctx) {
        try {
            var players = EntityArgument.getPlayers(ctx, "players");
            String id = StringArgumentType.getString(ctx, "id");
            int count = 0;
            for (ServerPlayer player : players) {
                ServerTipHelper.sendGeneral(id, player);
                count++;
            }

            Component message;
            if (count > 1) {
                message = Component.translatable("commands.tip.success.multiple", id, count);
            } else {
                message = Component.translatable("commands.tip.success.single", id, players.iterator().next().getDisplayName());
            }
            ctx.getSource().sendSuccess(() -> message, true);
            return count;
        } catch (Exception e) {
            Tip.LOGGER.error("Failed to display tip: ", e);
            ctx.getSource().sendFailure(Component.translatable("commands.tip.fail", e.getMessage()));
            return 0;
        }
    }

    private static int displayJson(CommandContext<CommandSourceStack> ctx) {
        try {
            var players = EntityArgument.getPlayers(ctx, "players");
            String json = StringArgumentType.getString(ctx, "json");
            Tip tip = TipHelper.parse(json).copy()
                    .temporary()
                    .build();
            int count = 0;
            for (ServerPlayer player : players) {
                ServerTipHelper.sendCustom(tip, player);
                count++;
            }

            String id = tip.contents().isEmpty() ? tip.id() : tip.contents().get(0);
            Component message;
            if (count > 1) {
                message = Component.translatable("commands.tip.success.multiple", id, count);
            } else {
                message = Component.translatable("commands.tip.success.single", id, players.iterator().next().getDisplayName());
            }
            ctx.getSource().sendSuccess(() -> message, true);
            return count;
        } catch (Exception e) {
            Tip.LOGGER.error("Failed to display tip: ", e);
            ctx.getSource().sendFailure(Component.translatable("commands.tip.fail", e.getMessage()));
            return 0;
        }
    }

    private static int displayCustom(CommandContext<CommandSourceStack> ctx) {
        try {
            var players = EntityArgument.getPlayers(ctx, "players");
            String title = StringArgumentType.getString(ctx, "title");
            String content = StringArgumentType.getString(ctx, "content");
            int displayTime = IntegerArgumentType.getInteger(ctx, "displayTime");
            Tip tip = toTip(title, content, displayTime);
            int count = 0;
            for (ServerPlayer player : players) {
                ServerTipHelper.sendCustom(tip, player);
                count++;
            }

            String id = tip.contents().isEmpty() ? tip.id() : tip.contents().get(0);
            Component message;
            if (count > 1) {
                message = Component.translatable("commands.tip.success.multiple", id, count);
            } else {
                message = Component.translatable("commands.tip.success.single", id, players.iterator().next().getDisplayName());
            }
            ctx.getSource().sendSuccess(() -> message, true);
            return count;
        } catch (Exception e) {
            Tip.LOGGER.error("Failed to display tip: ", e);
            ctx.getSource().sendFailure(Component.translatable("commands.tip.fail", e.getMessage()));
            return 0;
        }
    }

    public static Tip toTip(String title, String content, int displayTime) {
        List<String> contents = new ArrayList<>();
        if (!content.isEmpty()) {
            contents.addAll(Arrays.asList(content.split("\\$\\$")));
        }
        return Tip.builder(TipHelper.randomString())
                .contents(title)
                .contents(contents)
                .displayTime(displayTime)
                .alwaysVisible(displayTime <= -1)
                .temporary()
                .build();
    }
}
