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

import com.google.gson.JsonElement;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.tips.ServerTipSender;
import com.teammoeg.frostedheart.content.tips.Tip;
import com.teammoeg.frostedheart.content.tips.TipManager;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;

import static com.teammoeg.frostedheart.infrastructure.command.CMD.*;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TipCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        var server = literal("tip")
                        .then(literal("display").then(players("players").then(string("id").suggests(TipManager::suggest)
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

        if (FMLEnvironment.dist.isClient()) {
            var client = server.then(literal("client")
                    .then(literal("reload")
                            .executes(c -> {TipManager.INSTANCE.loadFromFile(); return Command.SINGLE_SUCCESS;}))
                    .then(literal("unlockAll")
                            .executes(c -> {TipManager.INSTANCE.state().unlockAll(); return Command.SINGLE_SUCCESS;}))
                    .then(literal("display").then(string("id").suggests(TipManager::suggest)
                            .executes(TipCommand::clientDisplay)))
                    .then(literal("displayCustom")
                            .then(literal("json").then(string("json")
                                    .executes(TipCommand::clientDisplayJson)))
                            .then(string("title").then(string("content").then(integer("displayTime")
                                    .executes(TipCommand::clientDisplayCustom))))));

            for (String string : new String[]{FHMain.MODID, FHMain.ALIAS, FHMain.TWRID}) {
                dispatcher.register(Commands.literal(string).then(client));
            }
            dispatcher.register(client);
        }
    }

    private static int display(CommandContext<CommandSourceStack> ctx) {
        try {
            var players = EntityArgument.getPlayers(ctx, "players");
            String id = StringArgumentType.getString(ctx, "id");
            int count = 0;
            for (ServerPlayer player : players) {
                ServerTipSender.sendGeneral(id, player);
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
            Tip tip = Tip.builder(String.valueOf(Util.getMillis()))
                    .fromJson(Tip.GSON.fromJson(json, JsonElement.class).getAsJsonObject())
                    .setTemporary()
                    .build();
            int count = 0;
            for (ServerPlayer player : players) {
                ServerTipSender.sendCustom(tip, player);
                count++;
            }

            String id = tip.getContents().isEmpty() ? tip.getId() : tip.getContents().get(0).getString();
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
                ServerTipSender.sendCustom(tip, player);
                count++;
            }

            String id = tip.getContents().isEmpty() ? tip.getId() : tip.getContents().get(0).getString();
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

    private static int clientDisplay(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        TipManager.INSTANCE.display().general(id);
        return Command.SINGLE_SUCCESS;
    }

    private static int clientDisplayJson(CommandContext<CommandSourceStack> ctx) {
        String json = StringArgumentType.getString(ctx, "json");
        Tip tip = Tip.builder(String.valueOf(Util.getMillis()))
                .fromJson(Tip.GSON.fromJson(json, JsonElement.class).getAsJsonObject())
                .setTemporary()
                .build();
        TipManager.INSTANCE.display().general(tip);
        return Command.SINGLE_SUCCESS;
    }

    private static int clientDisplayCustom(CommandContext<CommandSourceStack> ctx) {
        String title = StringArgumentType.getString(ctx, "title");
        String content = StringArgumentType.getString(ctx, "content");
        int displayTime = IntegerArgumentType.getInteger(ctx, "displayTime");
        Tip tip = toTip(title, content, displayTime);
        TipManager.INSTANCE.display().general(tip);
        return Command.SINGLE_SUCCESS;
    }

    public static Tip toTip(String title, String content, int displayTime) {
        List<Component> contents = new ArrayList<>();
        if (!content.isEmpty()) {
            for (String s : content.split("\\$\\$")) {
                contents.add(Component.translatable(s));
            }
        }
        return Tip.builder(String.valueOf(Util.getMillis()))
                .line(Components.str(title))
                .lines(contents)
                .displayTime(displayTime)
                .alwaysVisible(displayTime <= -1)
                .setTemporary()
                .build();
    }
}
