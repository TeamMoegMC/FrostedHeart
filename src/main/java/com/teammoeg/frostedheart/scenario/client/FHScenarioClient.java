package com.teammoeg.frostedheart.scenario.client;

import java.util.Map;

import com.teammoeg.frostedheart.scenario.ScenarioExecutor;
import com.teammoeg.frostedheart.scenario.ScenarioExecutor.ScenarioMethod;
import com.teammoeg.frostedheart.scenario.runner.ParagraphRunner;
import com.teammoeg.frostedheart.scenario.runner.ScenarioRunner;

public class FHScenarioClient {
	static ScenarioExecutor client=new ScenarioExecutor();

	public static void registerCommand(String cmdName, ScenarioMethod method) {
		client.registerCommand(cmdName, method);
	}

	public static void regiser(Class<?> clazz) {
		client.regiser(clazz);
	}

	public static void callCommand(String name, ScenarioRunner runner, Map<String, String> params) {
		client.callCommand(name, runner, params);
	}
}
