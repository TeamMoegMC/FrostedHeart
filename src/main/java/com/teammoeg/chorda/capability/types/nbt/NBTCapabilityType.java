/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.chorda.capability.types.nbt;

import org.objectweb.asm.Type;

import com.teammoeg.frostedheart.mixin.forge.CapabilityManagerAccess;
import com.teammoeg.chorda.capability.CapabilityStored;
import com.teammoeg.chorda.capability.types.CapabilityType;
import com.teammoeg.chorda.io.NBTSerializable;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;
/**
 * Basic nbt capability type
 * 
 * */
public class NBTCapabilityType<C extends NBTSerializable> implements CapabilityType<C>,CapabilityStored<C> {
	private Class<C> capClass;
	private Capability<C> capability;
	private NonNullSupplier<C> factory;
	public NBTCapabilityType(Class<C> capClass, NonNullSupplier<C> factory) {
		super();
		this.capClass = capClass;
		this.factory = factory;
	}
	@SuppressWarnings("unchecked")
	public void register() {
        capability=(Capability<C>) ((CapabilityManagerAccess)(Object)CapabilityManager.INSTANCE).getProviders().get(Type.getInternalName(capClass).intern());
	}
	public NBTCapabilityProvider<C> provider() {
		return new NBTCapabilityProvider<>(this);
	}
	LazyOptional<C> createCapability(){
		return LazyOptional.of(factory);
	}
	public LazyOptional<C> getCapability(Object cap) {
		if(cap instanceof ICapabilityProvider)
			return ((ICapabilityProvider)cap).getCapability(capability);
		return LazyOptional.empty();
	}
	public LazyOptional<C> getCapability(Object cap,Direction dir) {
		if(cap instanceof ICapabilityProvider)
			return ((ICapabilityProvider)cap).getCapability(capability,dir);
		return LazyOptional.empty();
	}
    public Capability<C> capability() {
		return capability;
	}
    public boolean isCapability(Capability<?> cap) {
		return capability==cap;
	}
	public Class<C> getCapClass() {
		return capClass;
	}
}
