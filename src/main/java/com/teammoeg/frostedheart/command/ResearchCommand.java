package com.teammoeg.frostedheart.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.ResearchData;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

public class ResearchCommand {
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		LiteralArgumentBuilder<CommandSource> add = Commands.literal("research")
				.then(Commands.literal("complete").then(Commands.argument("name",StringArgumentType.string()).suggests((ct,s)->{
					for(Research r:FHResearch.getAllResearch())
						if(r.getId().startsWith(s.getRemaining()))
							s.suggest(r.getId());
					return s.buildFuture();
				}).executes(ct->{
					Research rs=FHResearch.getResearch(ct.getArgument("name",String.class).toString()).get();
					if(rs==null) {
						ct.getSource().sendErrorMessage(new StringTextComponent("Research not found").mergeStyle(TextFormatting.RED));
						return Command.SINGLE_SUCCESS;
					}
					ResearchData rd=ResearchDataAPI.getData(ct.getSource().asPlayer()).getData(rs);
					rd.setFinished(true);
					rd.announceCompletion();
					ct.getSource().sendFeedback(new StringTextComponent("Succeed!").mergeStyle(TextFormatting.GREEN), false);
				return Command.SINGLE_SUCCESS;
				}))).then(Commands.literal("edit").then(Commands.argument("enable",BoolArgumentType.bool()).executes(ct->{
					FHResearch.editor=ct.getArgument("enable",Boolean.class);
					ct.getSource().sendFeedback(new StringTextComponent("Editing mode set"+String.valueOf(FHResearch.editor)).mergeStyle(TextFormatting.GREEN), false);
				return Command.SINGLE_SUCCESS;
				}))).then(Commands.literal("reset").then(Commands.argument("name",StringArgumentType.string()).suggests((ct,s)->{
					for(Research r:FHResearch.getAllResearch())
						if(r.getId().startsWith(s.getRemaining()))
							s.suggest(r.getId());
					return s.buildFuture();
				}).executes(ct->{
					ResearchDataAPI.getData(ct.getSource().asPlayer()).resetData(FHResearch.getResearch(ct.getArgument("name",String.class).toString()).get());
				return Command.SINGLE_SUCCESS;
				})));
		dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermissionLevel(2)).then(add));
	}
}
