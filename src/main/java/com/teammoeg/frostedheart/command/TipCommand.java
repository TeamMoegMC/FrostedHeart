package com.teammoeg.frostedheart.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.tips.network.DisplayTipPacket;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.PacketDistributor;

public class TipCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("tips").requires((p_198820_0_) -> {return p_198820_0_.hasPermissionLevel(2);}).then(
                Commands.literal("add").then(
                Commands.argument("targets", EntityArgument.players()).then(
                Commands.argument("name", StringArgumentType.string())
                        .executes((a) -> {
                            String ID = a.getArgument("name", String.class);
                            int i = 0;

                            for(ServerPlayerEntity sp : EntityArgument.getPlayers(a, "targets")) {
                                FHNetwork.send(PacketDistributor.PLAYER.with(() -> sp), new DisplayTipPacket(ID));
                                i++;
                            }

                            return i;
                        }
                ))))
        );
    }
}
