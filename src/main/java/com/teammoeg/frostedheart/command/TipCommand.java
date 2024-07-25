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
import com.teammoeg.frostedheart.util.TranslateUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.PacketDistributor;

public class TipCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> run = Commands.literal("tips").then(
            Commands.literal("add").then(
            Commands.argument("targets", EntityArgument.players()).then(
            Commands.argument("ID", StringArgumentType.string())
                .executes((c) -> {
                    String ID = c.getArgument("ID", String.class);
                    int i = 0;

                    for(ServerPlayerEntity sp : EntityArgument.getPlayers(c, "targets")) {
                        FHNetwork.send(PacketDistributor.PLAYER.with(() -> sp), new DisplayTipPacket(ID));
                        i++;
                    }

                    c.getSource().sendFeedback(TranslateUtils.translate("tips.frostedheart.command", i, ID), true);
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
                        for(ServerPlayerEntity sp : EntityArgument.getPlayers(c, "targets")) {
                            FHNetwork.send(PacketDistributor.PLAYER.with(() -> sp), new DisplayCustomTipPacket(title, content, visibleTime, history));
                            i++;
                        }

                        c.getSource().sendFeedback(TranslateUtils.translate("tips.frostedheart.command", i, title), true);
                        return i;
                    }
        ))))))));
        dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermissionLevel(2)).then(run));
    }
}
