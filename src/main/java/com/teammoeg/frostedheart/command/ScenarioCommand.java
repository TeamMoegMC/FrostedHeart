package com.teammoeg.frostedheart.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.runner.ActNamespace;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteTarget;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;

public class ScenarioCommand {
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		LiteralArgumentBuilder<CommandSource> run=Commands.literal("scenario").then(
			
			Commands.literal("jump").then(
			Commands.argument("scenario", StringArgumentType.string()).then(
				Commands.argument("label", StringArgumentType.string()).executes(ct->{
					FHScenario.get(ct.getSource().getPlayerOrException()).jump(
						new ExecuteTarget(FHScenario.get(ct.getSource().getPlayerOrException()),StringArgumentType.getString(ct, "scenario"),StringArgumentType.getString(ct, "label")));
					return Command.SINGLE_SUCCESS;
				})
				).executes(ct->{
					
					FHScenario.get(ct.getSource().getPlayerOrException()).jump(
						new ExecuteTarget(FHScenario.get(ct.getSource().getPlayerOrException()),StringArgumentType.getString(ct, "scenario"),null));
					return Command.SINGLE_SUCCESS;
				})
			)).then(Commands.literal("queue").then(
				Commands.argument("scenario", StringArgumentType.string()).then(
					Commands.argument("label", StringArgumentType.string()).executes(ct->{
						FHScenario.get(ct.getSource().getPlayerOrException()).queue(
							new ExecuteTarget(FHScenario.get(ct.getSource().getPlayerOrException()),StringArgumentType.getString(ct, "scenario"),StringArgumentType.getString(ct, "label")));
						return Command.SINGLE_SUCCESS;
					})
					).executes(ct->{
						FHScenario.get(ct.getSource().getPlayerOrException()).queue(
							new ExecuteTarget(FHScenario.get(ct.getSource().getPlayerOrException()),StringArgumentType.getString(ct, "scenario"),null));
						return Command.SINGLE_SUCCESS;
					})
			)).then(Commands.literal("run").then(
				Commands.argument("script", StringArgumentType.string()).executes(ct->{
                    String sb = StringArgumentType.getString(ct, "script") + "\n" + "@return";
						ScenarioConductor cdt=FHScenario.get(ct.getSource().getPlayerOrException());
						cdt.addCallStack();
						cdt.run(FHScenario.parser.parseString("<anoymous_command>", sb));
						return Command.SINGLE_SUCCESS;
					})
			)).then(Commands.literal("pause").executes(ct->{
				ScenarioConductor cdt=FHScenario.get(ct.getSource().getPlayerOrException());
				cdt.pauseAct();
				return Command.SINGLE_SUCCESS;
			})).then(Commands.literal("continue")
				.then(Commands.literal("chapter").then(
					Commands.literal("act").executes(ct->{
						ScenarioConductor cdt=FHScenario.get(ct.getSource().getPlayerOrException());
						cdt.continueAct(new ActNamespace(StringArgumentType.getString(ct, "chapter"),StringArgumentType.getString(ct, "act")));
						return Command.SINGLE_SUCCESS;
					})))
				);
		dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermission(2)).then(run));
	}

}
