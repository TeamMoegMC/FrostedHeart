/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.infrastructure.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.network.InfraredDiagnostics;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InfraredServerCommand {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fh_ir_server").executes(InfraredServerCommand::status)
                .then(Commands.literal("status").executes(InfraredServerCommand::status))
                .then(Commands.literal("trace").then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c -> {
                    boolean enabled=BoolArgumentType.getBool(c,"enabled");
                    InfraredDiagnostics.setTracing(c.getSource().getPlayerOrException(),enabled);
                    c.getSource().sendSuccess(() -> Component.literal("IR server trace="+enabled+"; see server latest.log"),false);return 1;
                }))));
    }

    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player=context.getSource().getPlayerOrException();
        var snapshot=MinecraftThermalInput.gameplayInfraredSnapshot(player,true,0,0,0,new long[12],false,new long[0],0,0);
        String report="IR_SERVER "+InfraredDiagnostics.summarize(snapshot);
        context.getSource().sendSuccess(() -> Component.literal(report),false);
        FHMain.LOGGER.info(report);
        return 1;
    }
}
