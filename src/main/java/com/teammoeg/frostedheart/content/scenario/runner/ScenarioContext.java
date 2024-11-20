package com.teammoeg.frostedheart.content.scenario.runner;

import java.util.HashMap;
import java.util.Map;

import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.parser.Scenario;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteStackElement;

import net.minecraft.world.entity.player.Player;

public class ScenarioContext {
	Player player;
	String lang;
	/** Variable data. */
	protected IScenarioVaribles varData=new DirectScenarioVariables();
	/** Macros. */
	Map<String,ExecuteStackElement> macros=new HashMap<>();
	public Player player() {
		return player;
	}
	public void sendMessage(String string) {
		// TODO Auto-generated method stub
		
	}
	public String getLang() {
		return lang;
	}
	public Scenario loadScenario(String name) {
		return FHScenario.loadScenario(this, name);
	}
	public IScenarioVaribles getVarData() {
		return varData;
	}
	public IScenarioVaribles getVaribles() {
		return varData;
	}
	
}
