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

package com.teammoeg.frostedheart.content.town.ui;

import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.frostedheart.FHNetwork;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 城镇界面的「详细模式」：按玩家保存的开关，开启后城镇界面多出调试页，显示原始小数、阈值和内部数值。
 * 只管显示，不影响玩法；与 {@code ITown.DEBUG_MODE}（玩法调试）无关。只有 2 级权限才能开启，失去权限后登录时自动关闭。
 * <p>
 * The town screens' detailed mode: a per-player switch that adds a debug tab with raw decimals, thresholds and
 * internal values. It only changes what is shown, never gameplay, and is unrelated to {@code ITown.DEBUG_MODE}.
 * Only permission level 2 may turn it on; a player who has lost that level has it cleared at login.
 */
public final class TownUiMode {
    /** Kept in the persisted part of the player's data, so it survives death and relogging. */
    public static final String TAG = "frostedheart:town_ui_detailed";
    public static final int PERMISSION = 2;

    private TownUiMode() {
    }

    public static boolean isDetailed(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(TAG);
    }

    /** Stores the switch in the player's persisted data without telling the client. */
    public static void store(Player player, boolean detailed) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (detailed) persisted.putBoolean(TAG, true);
        else persisted.remove(TAG);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    /** Stores the switch and tells that player's client. */
    public static void setDetailed(ServerPlayer player, boolean detailed) {
        store(player, detailed);
        sync(player);
    }

    /** Drops the switch of a player without the permission level; returns whether it was dropped. */
    public static boolean clearIfUnauthorized(Player player) {
        if (!isDetailed(player) || player.hasPermissions(PERMISSION)) return false;
        store(player, false);
        return true;
    }

    public static void sync(ServerPlayer player) {
        // Fake and GameTest mock players have no open connection to tell.
        if (player.connection == null || !player.connection.connection.isConnected()) return;
        FHNetwork.INSTANCE.sendPlayer(player, new TownUiModeS2CPacket(isDetailed(player)));
    }

    /** At login: drop the switch of a player who may no longer use it, then tell the client. */
    public static void onLogin(ServerPlayer player) {
        clearIfUnauthorized(player);
        sync(player);
    }

    /** {@code ui detailed [on|off] [targets]}, to be placed under the permission-checked town command. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("ui").then(Commands.literal("detailed")
                .requires(source -> source.hasPermission(PERMISSION))
                .executes(TownUiMode::report)
                .then(Commands.literal("on")
                        .executes(context -> set(context, List.of(context.getSource().getPlayerOrException()), true))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> set(context, EntityArgument.getPlayers(context, "targets"), true))))
                .then(Commands.literal("off")
                        .executes(context -> set(context, List.of(context.getSource().getPlayerOrException()), false))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> set(context, EntityArgument.getPlayers(context, "targets"), false)))));
    }

    private static int report(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        context.getSource().sendSuccess(() -> Component.translatable(
                isDetailed(player) ? "command.frostedheart.town.ui.detailed.on" : "command.frostedheart.town.ui.detailed.off",
                player.getDisplayName()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int set(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, boolean detailed) {
        for (ServerPlayer player : players) {
            setDetailed(player, detailed);
            context.getSource().sendSuccess(() -> Component.translatable(
                    detailed ? "command.frostedheart.town.ui.detailed.on" : "command.frostedheart.town.ui.detailed.off",
                    player.getDisplayName()), true);
        }
        return players.size();
    }
}
