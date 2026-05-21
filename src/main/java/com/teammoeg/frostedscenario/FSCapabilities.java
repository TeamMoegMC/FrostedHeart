package com.teammoeg.frostedscenario;

import static com.teammoeg.chorda.capability.CapabilityRegistry.register;

import com.teammoeg.chorda.capability.types.nbt.NBTCapabilityType;
import com.teammoeg.frostedscenario.runner.ScenarioConductor;

public class FSCapabilities {

	public static final NBTCapabilityType<ScenarioConductor> SCENARIO=register(ScenarioConductor.class);
	
	public static void setup() {
	
	}
}
