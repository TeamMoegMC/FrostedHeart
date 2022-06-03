package com.teammoeg.frostedheart.research.api;

import com.teammoeg.frostedheart.research.TeamResearchData;

import net.minecraft.nbt.CompoundNBT;

public class ClientResearchDataAPI {

	private ClientResearchDataAPI() {
	}
	public static TeamResearchData getData() {
		return TeamResearchData.getClientInstance();

	}
	public static CompoundNBT getVariants() {
		return TeamResearchData.getClientInstance().getVariants();

	}
}
