package com.teammoeg.frostedheart.scenario.runner;

import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.target.ParagraphTarget;

public class QuestExecuteTarget extends ParagraphTarget{
	ImmutableQuestNamespace ns;

	public QuestExecuteTarget(String name, int parNum, ImmutableQuestNamespace ns) {
		super(name, parNum);
		this.ns = ns;
	}

	public QuestExecuteTarget(Scenario sc, int parNum, ImmutableQuestNamespace ns) {
		super(sc, parNum);
		this.ns = ns;
	}

	@Override
	public void accept(ScenarioConductor runner) {
		super.accept(runner);
		runner.continueQuest(ns);
	}


	
}
