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
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class CommandHelper {
    public static final String[] COMMANDS = new String[]{FHMain.MODID, FHMain.ALIAS, FHMain.TWRID};
    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, LiteralArgumentBuilder<CommandSourceStack> command) {
        for (String string : COMMANDS) {
            dispatcher.register(Commands.literal(string).then(command));
        }
        dispatcher.register(command);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return Commands.literal(name);
    }
    public static RequiredArgumentBuilder<CommandSourceStack, ?> string(String name) {
        return Commands.argument(name, StringArgumentType.string());
    }
    public static RequiredArgumentBuilder<CommandSourceStack, ?> players(String name) {
        return Commands.argument(name, EntityArgument.players());
    }
    public static RequiredArgumentBuilder<CommandSourceStack, ?> integer(String name) {
        return Commands.argument(name, IntegerArgumentType.integer());
    }
    public static RequiredArgumentBuilder<CommandSourceStack, ?> bool(String name) {
        return Commands.argument(name, BoolArgumentType.bool());
    }



    public final CommandContext<CommandSourceStack> ctx;
    public CommandHelper(CommandContext<CommandSourceStack> ctx) {
        this.ctx = ctx;
    }

    public @NotNull Collection<ServerPlayer> getPlayers(String name) throws CommandSyntaxException {
        return EntityArgument.getPlayers(ctx, name);
    }
    public String getString(String name) {
        return StringArgumentType.getString(ctx, name);
    }
    public int getInt(String name) {
        return IntegerArgumentType.getInteger(ctx, name);
    }
    public boolean getBool(String name) {
        return BoolArgumentType.getBool(ctx, name);
    }

    public void sendSuccess(Component message) {
        ctx.getSource().sendSuccess(() -> message, true);
    }
    public void sendFailure(Component c) {
        ctx.getSource().sendFailure(c);
    }
}
