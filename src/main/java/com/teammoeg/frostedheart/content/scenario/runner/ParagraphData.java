package com.teammoeg.frostedheart.content.scenario.runner;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.scenario.parser.Scenario;
import com.teammoeg.frostedheart.content.scenario.runner.target.PreparedScenarioTarget;
import com.teammoeg.frostedheart.content.scenario.runner.target.ScenarioTarget;

public record ParagraphData(String name,int paragraphNum) implements ScenarioTarget{
	public static final Codec<ParagraphData> CODEC=RecordCodecBuilder.create(t->t.group(
		Codec.STRING.fieldOf("name").forGetter(o->o.name()),
		Codec.INT.fieldOf("paragraphNum").forGetter(o->o.paragraphNum())
		).apply(t,ParagraphData::new));
	public ParagraphData(ParagraphData old) {
		this(old.name(),old.paragraphNum());
	}
	
	@Override
	public PreparedScenarioTarget prepare(ScenarioContext t, Scenario current) {
		Scenario scenario=t.loadScenario(name);
		int nodeNum=0;
		if(paragraphNum<scenario.paragraphs().length&&paragraphNum>=0)
			nodeNum=scenario.paragraphs()[paragraphNum];
		return new PreparedScenarioTarget(scenario,nodeNum);
	}
}