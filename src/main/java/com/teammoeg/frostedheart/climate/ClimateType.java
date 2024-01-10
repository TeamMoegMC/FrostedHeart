package com.teammoeg.frostedheart.climate;

public enum ClimateType {
	NONE(0),
	SNOW_BLIZZARD(1),//A special snow before blizzard
	BLIZZARD(1),
	SNOW(2),
	SUN(3),
	CLOUDY(4);
	private ClimateType(int typeId) {
		this.typeId = typeId;
	}

	int typeId;//Same typeid represent same weather event but with different presentation, for forecasting
	
	
}
