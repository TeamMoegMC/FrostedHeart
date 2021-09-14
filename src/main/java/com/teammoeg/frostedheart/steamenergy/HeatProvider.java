package com.teammoeg.frostedheart.steamenergy;

public interface HeatProvider extends EnergyNetworkProvider{
	
	float getMaxHeat();
	float drainHeat(float value);
}
