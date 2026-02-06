package com.teammoeg.frostedheart.content.climate.gamedata.climate;

public record ClimateResult(float temperature,ClimateType climate) {
	public static final ClimateResult EMPTY=new ClimateResult(0f,ClimateType.NONE);
}
