package com.teammoeg.frostedresearch;

import com.teammoeg.chorda.dataholders.SpecialDataType;
import com.teammoeg.frostedresearch.data.TeamResearchData;

public class FRSpecialDataTypes {

	private FRSpecialDataTypes() {
	}
	public static final SpecialDataType<TeamResearchData> RESEARCH_DATA=new SpecialDataType<>("research",TeamResearchData::new,TeamResearchData.CODEC);
	public static void init() {}

}
