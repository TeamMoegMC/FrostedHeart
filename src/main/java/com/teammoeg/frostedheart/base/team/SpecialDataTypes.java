/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.base.team;

import java.util.HashSet;
import java.util.Set;

import com.teammoeg.frostedheart.content.heatdevice.generator.GeneratorData;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.town.TeamTownData;

public class SpecialDataTypes {

	public static final Set<SpecialDataType<?,?>> TYPE_REGISTRY=new HashSet<>();
	public static final SpecialDataType<TeamResearchData,TeamDataHolder> RESEARCH_DATA=new SpecialDataType<>("research",TeamResearchData::new);
	public static final SpecialDataType<GeneratorData,TeamDataHolder> GENERATOR_DATA=new SpecialDataType<>("generator",GeneratorData::new);
	public static final SpecialDataType<TeamTownData,TeamDataHolder> TOWN_DATA=new SpecialDataType<>("town",TeamTownData::new);

	public static void init() {}
}
