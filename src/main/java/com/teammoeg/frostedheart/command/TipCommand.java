package com.teammoeg.frostedheart.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.tips.network.DisplayCustomTipPacket;
import com.teammoeg.frostedheart.content.tips.network.DisplayTipPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class TipCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> run = Commands.literal("tips").then(
            Commands.literal("add").then(
            Commands.argument("targets", EntityArgument.players()).then(
            Commands.argument("ID", StringArgumentType.string())
                .executes((c) -> {
                    String ID = c.getArgument("ID", String.class);
                    int i = 0;

                    for(ServerPlayer sp : EntityArgument.getPlayers(c, "targets")) {
                        FHNetwork.send(PacketDistributor.PLAYER.with(() -> sp), new DisplayTipPacket(ID));
                        i++;
                    }

                    return i;
                }
                ))).then(
                Commands.literal("custom").then(
                Commands.argument("targets", EntityArgument.players()).then(
                Commands.argument("title", StringArgumentType.string()).then(
                Commands.argument("content", StringArgumentType.string()).then(
                Commands.argument("visible_time", IntegerArgumentType.integer()).then(
                Commands.argument("history", BoolArgumentType.bool())
                    .executes((c) -> {
                        String title = c.getArgument("title", String.class);
                        String content = c.getArgument("content", String.class);
                        Integer visibleTime = c.getArgument("visible_time", Integer.class);
                        boolean history = c.getArgument("history", Boolean.class);

                        int i = 0;
                        for(ServerPlayer sp : EntityArgument.getPlayers(c, "targets")) {
                            FHNetwork.send(PacketDistributor.PLAYER.with(() -> sp), new DisplayCustomTipPacket(title, content, visibleTime, history));
                            i++;
                        }

                        return i;
                    }
        ))))))));
        dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermission(2)).then(run));
    }
}
