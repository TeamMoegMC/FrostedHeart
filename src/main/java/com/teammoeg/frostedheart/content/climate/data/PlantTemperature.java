package com.teammoeg.frostedheart.content.climate.data;

import com.teammoeg.chorda.math.CMath;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public interface PlantTemperature {
	enum TemperatureType{
		SURVIVE,
		GROW,
		BONEMEAL;
	}
    PlantTemperature DEFAULT_PLANTS=new PlantTemperature() {

		@Override
		public float minFertilize() {
			return PlantTemperature.DEFAULT_BONEMEAL_TEMP;
		}

		@Override
		public float minGrow() {
			return PlantTemperature.DEFAULT_GROW_TEMP;
		}

		@Override
		public float minSurvive() {
			return PlantTemperature.DEFAULT_SURVIVE_TEMP;
		}

		@Override
		public float maxFertilize() {
			return PlantTemperature.DEFAULT_BONEMEAL_MAX_TEMP;
		}

		@Override
		public float maxGrow() {
			return PlantTemperature.DEFAULT_GROW_MAX_TEMP;
		}

		@Override
		public float maxSurvive() {
			return PlantTemperature.DEFAULT_GROW_MAX_TEMP;
		}

		@Override
		public boolean snowVulnerable() {
			return PlantTemperature.DEFAULT_SNOW_VULNERABLE;
		}

		@Override
		public boolean blizzardVulnerable() {
			return PlantTemperature.DEFAULT_BLIZZARD_VULNERABLE;
		}

		public Block dead() {
			return Blocks.DEAD_BUSH;
		}

		public boolean willDie() {
			return true;
		}
    	
    };
    PlantTemperature DEFAULT_SAPLINGS=new PlantTemperature() {

		@Override
		public float minFertilize() {
			return PlantTemperature.DEFAULT_BONEMEAL_TEMP;
		}

		@Override
		public float minGrow() {
			return -6;
		}

		@Override
		public float minSurvive() {
			return PlantTemperature.DEFAULT_SURVIVE_TEMP;
		}

		@Override
		public float maxFertilize() {
			return WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX;
		}

		@Override
		public float maxGrow() {
			return WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX;
		}

		@Override
		public float maxSurvive() {
			return WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX;
		}

		@Override
		public boolean snowVulnerable() {
			return false;
		}

		@Override
		public boolean blizzardVulnerable() {
			return true;
		}
		public boolean shouldShowSurvive() {
			return false;
		}

		public Block dead() {
			return Blocks.DEAD_BUSH;
		}

		public boolean willDie() {
			return true;
		}
    };
	float DEFAULT_GROW_TIME_GAME_DAYS = 3;
	float DEFAULT_BONEMEAL_TEMP = 10;
	float DEFAULT_GROW_TEMP = 0;
	float DEFAULT_SURVIVE_TEMP = -10;
	float DEFAULT_BONEMEAL_MAX_TEMP = 40;
	float DEFAULT_GROW_MAX_TEMP = 40;
	float DEFAULT_SURVIVE_MAX_TEMP = 40;
	boolean DEFAULT_SNOW_VULNERABLE = true;
	boolean DEFAULT_BLIZZARD_VULNERABLE = true;
	float minFertilize();

	float minGrow();

	float minSurvive();

	float maxFertilize();

	float maxGrow();

	float maxSurvive();

	boolean snowVulnerable();

	boolean blizzardVulnerable();

	Block dead();

	boolean willDie();
	
	default boolean shouldShowSurvive() {
		return true;
	}
	default boolean shouldShowFertilize() {
		return true;
	}
	default boolean isValidTemperature(TemperatureType type,float temperature) {
		return CMath.inRange(temperature, min(type), max(type));
	}
	default float max(TemperatureType type) {
		return switch(type) {
		case SURVIVE->maxSurvive();
		case GROW->maxGrow();
		case BONEMEAL->maxFertilize();
		};
	}
	default float min(TemperatureType type) {
		return switch(type) {
		case SURVIVE->minSurvive();
		case GROW->minGrow();
		case BONEMEAL->minFertilize();
		};
	}
}