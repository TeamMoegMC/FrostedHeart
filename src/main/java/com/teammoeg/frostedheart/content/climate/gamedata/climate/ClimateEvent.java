package com.teammoeg.frostedheart.content.climate.gamedata.climate;

public interface ClimateEvent {

	long getStartTime();

	long getCalmEndTime();

	ClimateResult getHourClimate(long t);

	/**
	 * Compute the temperature at a given time according to this temperature event.
	 * This algorithm is based on a piecewise interpolation technique.
	 *
	 * @param t given in seconds.
	 * @return temperature at given time.
	 * @author JackyWangMislantiaJnirvana <wmjwld@live.cn>
	 */
	float getHourTemp(long t);

}