/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.infrastructure.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.runner.ActNamespace;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteTarget;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ScenarioCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> run=Commands.literal("scenario").then(
			
			Commands.literal("jump").then(
			Commands.argument("scenario", StringArgumentType.string()).then(
				Commands.argument("label", StringArgumentType.string()).executes(ct->{
					FHScenario.get(ct.getSource().getPlayerOrException()).jump(
						new ExecuteTarget(StringArgumentType.getString(ct, "scenario"),StringArgumentType.getString(ct, "label")));
					return Command.SINGLE_SUCCESS;
				})
				).executes(ct->{
					
					FHScenario.get(ct.getSource().getPlayerOrException()).jump(
						new ExecuteTarget(StringArgumentType.getString(ct, "scenario"),null));
					return Command.SINGLE_SUCCESS;
				})
			)).then(Commands.literal("queue").then(
				Commands.argument("scenario", StringArgumentType.string()).then(
					Commands.argument("label", StringArgumentType.string()).executes(ct->{
						FHScenario.get(ct.getSource().getPlayerOrException()).queue(
							new ExecuteTarget(StringArgumentType.getString(ct, "scenario"),StringArgumentType.getString(ct, "label")));
						return Command.SINGLE_SUCCESS;
					})
					).executes(ct->{
						FHScenario.get(ct.getSource().getPlayerOrException()).queue(
							new ExecuteTarget(StringArgumentType.getString(ct, "scenario"),null));
						return Command.SINGLE_SUCCESS;
					})
			)).then(Commands.literal("run").then(
				Commands.argument("script", StringArgumentType.string()).executes(ct->{
                    String sb = StringArgumentType.getString(ct, "script") + "\n" + "@return";
						ScenarioConductor cdt=FHScenario.get(ct.getSource().getPlayerOrException());
						cdt.getCurrentAct().addCallStack();
						cdt.getCurrentAct().run(FHScenario.parser.parseString("<anoymous_command>", sb+"\r\n@return"));
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
