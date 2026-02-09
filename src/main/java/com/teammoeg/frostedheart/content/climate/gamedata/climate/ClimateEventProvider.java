package com.teammoeg.frostedheart.content.climate.gamedata.climate;

import net.minecraft.util.RandomSource;

@FunctionalInterface
public interface ClimateEventProvider {
	ClimateEvent generateEvent(RandomSource randomSource,long startTime);
}
