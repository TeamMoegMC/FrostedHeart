package com.teammoeg.chorda.mixin;

import java.util.IdentityHashMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;

@Mixin(CapabilityManager.class)
public interface CapabilityManagerAccess {
	@Accessor(value="providers", remap=false)
	IdentityHashMap<String, Capability<?>> getProviders();
}
