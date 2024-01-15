package com.teammoeg.frostedheart.scenario.runner;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.target.IScenarioTarget;

public class ParagraphData implements IScenarioTarget{
	private String name;
	private transient Scenario sp;
	private int paragraphNum;
	public ParagraphData() {
		super();
	}
	public ParagraphData(String name, int paragraphNum) {
		super();
		this.name = name;
		this.paragraphNum = paragraphNum;
	}
	public ParagraphData copy() {
		return new ParagraphData(name,paragraphNum);
	}
	public void setScenario(Scenario sc) {
		this.sp=sc;
		this.name=sc.name;
	}
	public Scenario getScenario() {
		if(sp==null)
			sp=FHScenario.loadScenario(name);
		return sp;
	}

	public int getParagraphNum() {
		return paragraphNum;
	}
	public void setParagraphNum(int paraGraphNum) {
		this.paragraphNum = paraGraphNum;
	}
	public String getName() {
		return name;
	}
	@Override
	public void accept(ScenarioConductor t) {
		if(!getScenario().equals(t.getScenario())) {
			t.setScenario(getScenario());
			t.gotoNode(0);
		}
		if(paragraphNum!=0)
			t.gotoNode(t.getScenario().paragraphs[paragraphNum-1]);
	}
}