package com.teammoeg.frostedheart.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;

public class AddTempCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> add =
        		Commands.literal("settemp")
        			.then(Commands.argument("position",BlockPosArgument.blockPos())
        				.then(Commands.argument("range",IntegerArgumentType.integer())
        				.executes((ct) -> {
                			ChunkData.removeTempAdjust(ct.getSource().getWorld(),
                					BlockPosArgument.getBlockPos(ct,"position"));
                			return Command.SINGLE_SUCCESS;
        				})
        				.then(Commands.argument("temperature",IntegerArgumentType.integer())
        						.executes((ct) -> {
        			ChunkData.addCubicTempAdjust(ct.getSource().getWorld(),
        					BlockPosArgument.getBlockPos(ct,"position"),
        					IntegerArgumentType.getInteger(ct,"range"),
        					(byte)IntegerArgumentType.getInteger(ct,"temperature"));
        			return Command.SINGLE_SUCCESS;
        }))));
        dispatcher.register(Commands.literal(FHMain.MODID)
        		.requires(s->s.hasPermissionLevel(2))
        		.then(add));
    }
}
