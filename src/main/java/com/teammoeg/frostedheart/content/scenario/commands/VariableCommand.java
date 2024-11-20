package com.teammoeg.frostedheart.content.scenario.commands;

import com.teammoeg.frostedheart.content.scenario.Param;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;

public class VariableCommand {

	public static void eval(ScenarioCommandContext runner,@Param("v")String v,@Param("exp")String exp) {
		runner.context().getVaribles().setPathNumber(v, runner.eval(exp));
	}
	public static void set(ScenarioCommandContext runner,@Param("v")String v,@Param("val")String exp) {
		runner.context().getVaribles().setPathString(v, exp);
	}
}
