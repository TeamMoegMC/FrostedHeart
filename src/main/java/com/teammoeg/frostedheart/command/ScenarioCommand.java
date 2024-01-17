package com.teammoeg.frostedheart.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.runner.ActNamespace;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.scenario.runner.target.ExecuteTarget;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;

public class ScenarioCommand {
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		LiteralArgumentBuilder<CommandSource> run=Commands.literal("scenario").then(
			
			Commands.literal("jump").then(
			Commands.argument("scenario", StringArgumentType.string()).then(
				Commands.argument("label", StringArgumentType.string()).executes(ct->{
					FHScenario.runners.get(ct.getSource().asPlayer()).jump(
						new ExecuteTarget(FHScenario.runners.get(ct.getSource().asPlayer()),StringArgumentType.getString(ct, "scenario"),StringArgumentType.getString(ct, "label")));
					return Command.SINGLE_SUCCESS;
				})
				).executes(ct->{
					FHScenario.runners.get(ct.getSource().asPlayer()).jump(
						new ExecuteTarget(FHScenario.runners.get(ct.getSource().asPlayer()),StringArgumentType.getString(ct, "scenario"),null));
					return Command.SINGLE_SUCCESS;
				})
			)).then(Commands.literal("queue").then(
				Commands.argument("scenario", StringArgumentType.string()).then(
					Commands.argument("label", StringArgumentType.string()).executes(ct->{
						FHScenario.runners.get(ct.getSource().asPlayer()).queue(
							new ExecuteTarget(FHScenario.runners.get(ct.getSource().asPlayer()),StringArgumentType.getString(ct, "scenario"),StringArgumentType.getString(ct, "label")));
						return Command.SINGLE_SUCCESS;
					})
					).executes(ct->{
						FHScenario.runners.get(ct.getSource().asPlayer()).queue(
							new ExecuteTarget(FHScenario.runners.get(ct.getSource().asPlayer()),StringArgumentType.getString(ct, "scenario"),null));
						return Command.SINGLE_SUCCESS;
					})
			)).then(Commands.literal("run").then(
				Commands.argument("script", StringArgumentType.string()).executes(ct->{
						StringBuilder sb=new StringBuilder(StringArgumentType.getString(ct, "script"));
						sb.append("\n").append("@return");
						ScenarioConductor cdt=FHScenario.runners.get(ct.getSource().asPlayer());
						cdt.addCallStack();
						cdt.run(FHScenario.parser.parseString("<anoymous_command>", sb.toString()));
						return Command.SINGLE_SUCCESS;
					})
			)).then(Commands.literal("pause").executes(ct->{
				ScenarioConductor cdt=FHScenario.runners.get(ct.getSource().asPlayer());
				cdt.pauseAct();
				return Command.SINGLE_SUCCESS;
			})).then(Commands.literal("continue")
				.then(Commands.literal("chapter").then(
					Commands.literal("act").executes(ct->{
						ScenarioConductor cdt=FHScenario.runners.get(ct.getSource().asPlayer());
						cdt.continueAct(new ActNamespace(StringArgumentType.getString(ct, "chapter"),StringArgumentType.getString(ct, "act")));
						return Command.SINGLE_SUCCESS;
					})))
				);
		dispatcher.register(Commands.literal(FHMain.MODID).requires(s -> s.hasPermissionLevel(2)).then(run));
	}

}
