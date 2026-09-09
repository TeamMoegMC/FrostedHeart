/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.player.thermalitem.WarmStoneGateBPacketCounter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.teammoeg.frostedheart.infrastructure.command.CommandHelper.literal;
import static com.teammoeg.frostedheart.infrastructure.command.CommandHelper.registerCommand;

/** Client-only controls for the default-off warm-stone Gate B packet observer. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WarmStoneGateBClientCommand {
    private WarmStoneGateBClientCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        var command = literal("fh_gate_b")
                .then(literal("start").executes(WarmStoneGateBClientCommand::start))
                .then(literal("status").executes(WarmStoneGateBClientCommand::status))
                .then(literal("reset").executes(WarmStoneGateBClientCommand::reset))
                .then(literal("stop").executes(WarmStoneGateBClientCommand::stop));
        registerCommand(event.getDispatcher(), command);
    }

    private static int start(CommandContext<CommandSourceStack> context) {
        return send(context, "Gate B packet observer started: ",
                WarmStoneGateBPacketCounter.start());
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        return send(context, "Gate B packet observer status: ",
                WarmStoneGateBPacketCounter.snapshot());
    }

    private static int reset(CommandContext<CommandSourceStack> context) {
        return send(context, "Gate B packet observer reset: ",
                WarmStoneGateBPacketCounter.reset());
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        return send(context, "Gate B packet observer stopped: ",
                WarmStoneGateBPacketCounter.stop());
    }

    private static int send(
            CommandContext<CommandSourceStack> context,
            String prefix,
            WarmStoneGateBPacketCounter.Snapshot snapshot
    ) {
        context.getSource().sendSuccess(
                () -> Component.literal(prefix + snapshot.toLogLine()), false);
        return Command.SINGLE_SUCCESS;
    }
}
