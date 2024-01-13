package com.teammoeg.frostedheart.scenario.commands;

import java.util.HashMap;
import java.util.Map;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

public class ControlCommands {
	public void wt(ScenarioConductor runner,@Param("t")int t) {
		runner.waitClient();
		runner.addWait(t);
		runner.prepareTextualModification();

	}
	public void s(ScenarioConductor runner) {
		runner.stop();

	}
	public void jump(ScenarioConductor runner,@Param("s")String scenario,@Param("l")String label) {
		runner.jump(scenario, label);

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
