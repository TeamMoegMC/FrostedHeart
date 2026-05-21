package com.teammoeg.frostedresearch.compat.scenario;

import com.teammoeg.frostedresearch.api.ResearchDataAPI;
import com.teammoeg.frostedscenario.Param;
import com.teammoeg.frostedscenario.ScenarioCommandProvider;
import com.teammoeg.frostedscenario.runner.ScenarioCommandContext;
@ScenarioCommandProvider
public class ResearchCommands {

	public void setResearchAttribute(ScenarioCommandContext runner, @Param("k") String key, @Param("v") double value) {
		ResearchDataAPI.putVariantDouble(runner.context().player(), key, value);
	}
}
