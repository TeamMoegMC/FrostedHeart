package com.teammoeg.frostedheart.scenario.commands;

import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.runner.ScenarioVM;

public class VariableCommand {

	public static void eval(ScenarioVM runner,@Param("v")String v,@Param("exp")String exp) {
		runner.getVaribles().setPathNumber(v, runner.eval(exp));
	}
	public static void set(ScenarioVM runner,@Param("v")String v,@Param("val")String exp) {
		runner.getVaribles().setPathString(v, exp);
	}
}
