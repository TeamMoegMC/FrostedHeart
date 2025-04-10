package com.teammoeg.frostedresearch.compat;

import com.simibubi.create.content.kinetics.BlockStressValues;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.FRStress;

public class CreateCompat {
	public static void init() {
		   BlockStressValues.registerProvider(FRMain.MODID, new FRStress());
	}
}
