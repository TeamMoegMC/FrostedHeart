package com.teammoeg.frostedheart.mixin.forge;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.IdentityHashMap;

@Mixin(CapabilityManager.class)
public interface CapabilityManagerAccess {
	@Accessor(value="providers", remap=false)
	IdentityHashMap<String, Capability<?>> getProviders();
}
