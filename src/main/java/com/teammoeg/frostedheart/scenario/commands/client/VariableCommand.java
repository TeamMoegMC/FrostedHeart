package com.teammoeg.frostedheart.scenario.commands.client;

import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

public class VariableCommand {

	public static void eval(ScenarioConductor runner,@Param("v")String v,@Param("exp")String exp) {
		runner.getVaribles().setPathNumber(v, runner.eval(exp));
	}
	public static void set(ScenarioConductor runner,@Param("v")String v,@Param("val")String exp) {
		runner.getVaribles().setPathString(v, exp);
	}
}
