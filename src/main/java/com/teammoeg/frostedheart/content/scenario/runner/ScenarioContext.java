package com.teammoeg.frostedheart.content.scenario.runner;

import java.util.HashMap;
import java.util.Map;

import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.parser.Scenario;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteStackElement;

import net.minecraft.world.entity.player.Player;

public abstract class ScenarioContext {
	/** Variable data. */
	protected IScenarioVaribles varData=new DirectScenarioVariables();
	/** Macros. */
	Map<String,ExecuteStackElement> macros=new HashMap<>();
	public abstract Player player();
	public abstract void sendMessage(String string);
	public abstract String getLang();
	public Scenario loadScenario(String name) {
		return FHScenario.loadScenario(this, name);
	}
	public IScenarioVaribles getVarData() {
		return varData;
	}
	public IScenarioVaribles getVaribles() {
		return varData;
	}
	
	/**
	 * Adds the macro.
	 *
	 * @param name the name
	 */
	public void addMacro(String name,ScenarioThread thread) {
		macros.put(name.toLowerCase(), thread.getCurrentPosition(1));
	}
	public void takeSnapshot() {
		varData.takeSnapshot();
	}
	public void restoreSnapshot() {
		varData.restoreSnapshot();
	}
}
