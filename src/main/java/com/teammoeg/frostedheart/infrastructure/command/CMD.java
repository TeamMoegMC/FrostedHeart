package com.teammoeg.frostedheart.infrastructure.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

public class CMD {
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
}
