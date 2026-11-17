/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.infrastructure.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.render.InfraredViewRenderer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InfraredClientCommand {
    @SubscribeEvent public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fh_ir")
                .executes(c -> { c.getSource().sendSuccess(() -> Component.literal(InfraredViewRenderer.diagnosticStatus()),false); return 1; })
                .then(Commands.literal("status").executes(c -> {
                    c.getSource().sendSuccess(() -> Component.literal(InfraredViewRenderer.diagnosticStatus()),false); return 1;
                }))
                .then(Commands.literal("trace").then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c -> {
                    boolean enabled=BoolArgumentType.getBool(c,"enabled");
                    InfraredViewRenderer.setDiagnosticTrace(enabled);
                    c.getSource().sendSuccess(() -> Component.literal("IR client trace="+enabled+"; see latest.log"),false); return 1;
                })))
                .then(Commands.literal("resync").executes(c -> {
                    InfraredViewRenderer.invalidateDisplay();
                    c.getSource().sendSuccess(() -> Component.literal("IR full resync requested; leave infrared enabled"),false); return 1;
                })));
    }
}
