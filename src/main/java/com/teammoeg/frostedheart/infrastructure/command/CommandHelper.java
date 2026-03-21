package com.teammoeg.frostedheart.infrastructure.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class CommandHelper {
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
