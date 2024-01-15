package com.teammoeg.frostedheart.scenario.commands;

import java.util.HashMap;
import java.util.Map;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.runner.RunStatus;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

public class ControlCommands {
	public void delay(ScenarioConductor runner,@Param("t")int t) {
		//runner.waitClient();
		runner.getScene().addWait(t);
		runner.prepareTextualModification();

	}
	public void wt(ScenarioConductor runner) {
		runner.getCurrentAct().setStatus(RunStatus.WAITTRIGGER);
	}
	public void s(ScenarioConductor runner) {
		runner.stop();
	}
	public void l(ScenarioConductor runner) {
		runner.getScene().sendNoreline();
		runner.getScene().waitClient();
	}
	public void jump(ScenarioConductor runner,@Param("s")String scenario,@Param("l")String label) {
		runner.jump(scenario, label);

	}
	public void call(ScenarioConductor runner,@Param("s")String scenario,@Param("l")String label) {
		runner.call(scenario, label);

	}
	public void Return(ScenarioConductor runner) {
		runner.popCallStack();
	}
	public void macro(ScenarioConductor runner,@Param("s")String scenario,@Param("l")String label) {
		runner.call(scenario, label);

	}
	public void endmacro(ScenarioConductor runner) {
		runner.getExecutionData().remove("mp");
		runner.popCallStack();
	}
	public void link(ScenarioConductor runner,@Param("lid")String linkId,@Param("s")String scenario,@Param("l")String label) {
		runner.prepareTextualModification();
		linkId=runner.createLink(linkId, scenario, label);
		Map<String,String> pars=new HashMap<>();
		pars.put("lid", linkId);
		FHScenario.callClientCommand("link", runner, pars);
	}
	public void endlink(ScenarioConductor runner) {
		runner.prepareTextualModification();
		Map<String,String> pars=new HashMap<>();
		FHScenario.callClientCommand("endlink", runner, pars);
	}
	public void nolink(ScenarioConductor runner) {
		runner.clearLink();
	}
}
