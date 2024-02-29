package com.teammoeg.frostedheart.team;

import java.util.HashSet;
import java.util.Set;

import com.teammoeg.frostedheart.content.generator.GeneratorData;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.town.TeamTownData;

public class SpecialDataTypes {

	public static final Set<SpecialDataType<?,?>> TYPE_REGISTRY=new HashSet<>();
	public static final SpecialDataType<TeamResearchData,TeamDataHolder> RESEARCH_DATA=new SpecialDataType<>("research",TeamResearchData::new);
	public static final SpecialDataType<GeneratorData,TeamDataHolder> GENERATOR_DATA=new SpecialDataType<>("generator",GeneratorData::new);
	public static final SpecialDataType<TeamTownData,TeamDataHolder> TOWN_DATA=new SpecialDataType<>("town",TeamTownData::new);

	public static void init() {}
}
