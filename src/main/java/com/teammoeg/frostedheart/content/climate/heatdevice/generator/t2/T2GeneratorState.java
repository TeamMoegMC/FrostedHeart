package com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2;

import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorState;
import com.teammoeg.frostedheart.content.steamenergy.HeatEnergyNetwork;

import net.minecraftforge.fluids.capability.templates.FluidTank;

public class T2GeneratorState extends GeneratorState {
    HeatEnergyNetwork manager;
    int liquidtick = 0;
    int noliquidtick = 0;
    int tickUntilStopBoom = 20;
    int notFullPowerTick = 0;

    public static final int TANK_CAPACITY=200 * 1000;
    final int nextBoom = 200; //10s

    public FluidTank tank = new FluidTank(TANK_CAPACITY,
            f -> GeneratorSteamRecipe.findRecipe(f) != null);
	public T2GeneratorState() {
	}

}
