/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.bootstrap.common;

import com.teammoeg.chorda.dataholders.SpecialDataType;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorData;
import com.teammoeg.frostedheart.content.scenario.client.ClientStoredFlags;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.world.dimensionalseed.DimensionalSeed;

public class FHSpecialDataTypes {


	public static final SpecialDataType<GeneratorData> GENERATOR_DATA=new SpecialDataType<>("generator",GeneratorData::new,GeneratorData.CODEC);
	public static final SpecialDataType<TeamTownData> TOWN_DATA=new SpecialDataType<>("town",TeamTownData::new,TeamTownData.CODEC);
	public static final SpecialDataType<ClientStoredFlags> SCENARIO_COOKIES=new SpecialDataType<>("scenario",ClientStoredFlags::new,ClientStoredFlags.CODEC);
	public static final SpecialDataType<ClientStoredFlags> COOKIES=new SpecialDataType<>("scenario",ClientStoredFlags::new,ClientStoredFlags.CODEC);
	public static final SpecialDataType<DimensionalSeed> SEED=new SpecialDataType<>("seed",DimensionalSeed::new,DimensionalSeed.CODEC);
	
	public static void init() {}
}
