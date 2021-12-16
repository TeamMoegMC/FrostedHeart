package com.teammoeg.frostedheart.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.climate.chunkdata.ITemperatureAdjust;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.ResearchData;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;

import java.util.Collection;

public class ResearchCommand {
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		LiteralArgumentBuilder<CommandSource> add = Commands.literal("research")
				.then(Commands.literal("complete").then(Commands.argument("name",StringArgumentType.string()).executes(ct->{
					ResearchData rd=ResearchDataAPI.getData(ct.getSource().asPlayer()).getData(FHResearch.getResearch(ct.getArgument("name",String.class).toString()).get());
					rd.setFinished(true);
					rd.announceCompletion();
				return Command.SINGLE_SUCCESS;
				}))).then(Commands.literal("reset").then(Commands.argument("name",StringArgumentType.string()).executes(ct->{
					ResearchData rd=ResearchDataAPI.getData(ct.getSource().asPlayer()).getData(FHResearch.getResearch(ct.getArgument("name",String.class).toString()).get());
					rd.setFinished(false);
					rd.announceCompletion();
				return Command.SINGLE_SUCCESS;
				})));
		dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermissionLevel(2)).then(add));
	}
}
