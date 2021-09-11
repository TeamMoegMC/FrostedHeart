package com.teammoeg.frostedheart.steamenergy;

public class SteamEnergyNetwork {
	HeatProvider provider;
	public boolean drainHeat(float val) {
		return provider.drainHeat(val);
	}
}
