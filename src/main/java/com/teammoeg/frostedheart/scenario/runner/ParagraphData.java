package com.teammoeg.frostedheart.scenario.runner;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.target.IScenarioTarget;

public class ParagraphData implements IScenarioTarget{
	private String name;
	private transient Scenario sp;
	private int paragraphNum;
	private transient IScenarioThread cd;
	public ParagraphData(IScenarioThread cd) {
		super();
		this.cd=cd;
	}
	public ParagraphData(IScenarioThread cd,String name, int paragraphNum) {
		this(cd);
		this.name = name;
		this.paragraphNum = paragraphNum;
	}
	public ParagraphData copy() {
		return new ParagraphData(cd,name,paragraphNum);
	}
	public void setScenario(Scenario sc) {
		this.sp=sc;
		this.name=sc.name;
	}
	public Scenario getScenario() {
		if(sp==null)
			sp=FHScenario.loadScenario(cd,name);
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
	public void apply(IScenarioThread t) {
		if(name!=null&&!getScenario().equals(t.getScenario())) {
			t.setScenario(getScenario());
			t.setNodeNum(0);
		}
		if(paragraphNum!=0&&paragraphNum<=t.getScenario().paragraphs.length)
			t.setNodeNum(t.getScenario().paragraphs[paragraphNum-1]+1);
	}
}