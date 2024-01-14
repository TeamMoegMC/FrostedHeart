package com.teammoeg.frostedheart.scenario.runner.target;

import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

public class ParagraphTarget extends ScenarioTarget {
	private final int parNum;

	public ParagraphTarget(String name, int parNum) {
		super(name);
		this.parNum = parNum;
	}

	public ParagraphTarget(Scenario sc, int parNum) {
		super(sc);
		this.parNum = parNum;
	}

	@Override
	public void accept(ScenarioConductor runner) {
		super.accept(runner);
		if(parNum!=0)
			runner.gotoNode(runner.getScenario().paragraphs[parNum-1]);
	}
	
}
