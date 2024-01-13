package com.teammoeg.frostedheart.scenario.runner;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.parser.Scenario;

public class ParagraphData{
	private String name;
	private transient Scenario sp;
	private int paragraphNum;
	public ParagraphData() {
		super();
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
}