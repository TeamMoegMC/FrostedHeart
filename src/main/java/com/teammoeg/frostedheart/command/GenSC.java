package com.teammoeg.frostedheart.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.world.FHFeatures;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.world.server.ServerWorld;

public class GenSC {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> add = Commands.literal("gen")
                .then(Commands.literal("ac").executes(ct -> {
                    FHFeatures.spacecraft_feature.generate(((ServerWorld) ct.getSource().asPlayer().world), ((ServerWorld) ct.getSource().asPlayer().world).getChunkProvider().getChunkGenerator(), ct.getSource().asPlayer().world.rand,
                            ct.getSource().asPlayer().getPosition());
                    return Command.SINGLE_SUCCESS;
                }));
        dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermissionLevel(2)).then(add));
    }
}
