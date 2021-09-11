package com.teammoeg.frostedheart.steamenergy;

public interface HeatProvider extends EnergyNetworkProvider{
	
	float getMaxHeat();
	boolean drainHeat(float value);
}
