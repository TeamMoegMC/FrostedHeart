package com.teammoeg.frostedheart.scenario.commands;

import java.util.HashMap;
import java.util.Map;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.Param;
import com.teammoeg.frostedheart.scenario.runner.RunStatus;
import com.teammoeg.frostedheart.scenario.runner.ScenarioVM;

public class TextualCommands {
	public void nowait(ScenarioVM runner) {
		runner.sendCachedSence();
		runner.getScene().isNowait=true;
	}
	public void endnowait(ScenarioVM runner) {
		runner.sendCachedSence();
		runner.getScene().isNowait=false;
	}
	public void r(ScenarioVM runner) {
		runner.newLine();
	}
	public void link(ScenarioVM runner,@Param("lid")String linkId,@Param("s")String scenario,@Param("l")String label) {
		runner.sendCachedSence();
		linkId=runner.createLink(linkId, scenario, label);
		Map<String,String> pars=new HashMap<>();
		pars.put("lid", linkId);
		FHScenario.callClientCommand("link", runner, pars);
	}
	public void endlink(ScenarioVM runner) {
		runner.sendCachedSence();
		Map<String,String> pars=new HashMap<>();
		FHScenario.callClientCommand("endlink", runner, pars);
	}
	public void nolink(ScenarioVM runner) {
		runner.clearLink();
	}
	public void delay(ScenarioVM runner,@Param("t")int t) {
		runner.getScene().addWait(t);
		runner.sendCachedSence();
	}
	public void er(ScenarioVM runner) {
		runner.getScene().clear();
		runner.sendCachedSence();
	}
	public void l(ScenarioVM runner) {
		runner.getScene().waitClient(true);
		runner.sendCachedSence();
	}
	public void wc(ScenarioVM runner) {
		runner.getScene().waitClient(false);
		runner.sendCachedSence();
	}
	public void wt(ScenarioVM runner) {
		runner.setStatus((RunStatus.WAITTRIGGER));
		runner.sendCachedSence();
	}
	public void wa(ScenarioVM runner) {
		runner.setStatus((RunStatus.WAITACTION));
		runner.sendCachedSence();
	}
	public void s(ScenarioVM runner) {
		runner.stop();
		runner.getScene().clear();
	}

}
