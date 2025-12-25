package com.teammoeg.chorda.capability;

import net.minecraftforge.common.capabilities.Capability;

public interface CapabilityStored<T> {
	Capability<T> capability();
}
