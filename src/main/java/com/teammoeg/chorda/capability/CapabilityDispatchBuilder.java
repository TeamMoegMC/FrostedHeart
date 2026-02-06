/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.chorda.capability;

import java.util.IdentityHashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.teammoeg.chorda.capability.types.nonpresistent.TransientCapability;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;
/**
 * A simple way to dispatch capability for initCapabilities.
 * Each provider should only have a single capabiltiy type, Each capability should only have a single provider
 * */
public class CapabilityDispatchBuilder {
	public static class CapabilityDispatcher implements ICapabilityProvider{
		Map<Capability<?>,ICapabilityProvider> caps;
		
		private CapabilityDispatcher(Map<Capability<?>, ICapabilityProvider> caps) {
			super();
			this.caps = caps;
		}

		@Override
		public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
			ICapabilityProvider prov=caps.get(cap);
			if(prov==null)
				return LazyOptional.empty();
			return prov.getCapability(cap,side);
		}
		
	}
	Map<Capability<?>,ICapabilityProvider> caps=new IdentityHashMap<>();
	public static CapabilityDispatchBuilder builder() {
		return new CapabilityDispatchBuilder();
	}
	public <C> CapabilityDispatchBuilder add(Capability<C> cap,NonNullSupplier<C> provider) {
		add(cap,LazyOptional.of(provider));
		return this;
	}
	public <C> CapabilityDispatchBuilder add(Capability<C> cap,LazyOptional<C> provider) {
		add(cap, new ICapabilityProvider() {
			@Override
			public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capin, @Nullable Direction side) {
				return capin==cap?provider.cast():LazyOptional.empty();
			}
		});
		return this;
	}
	public <T> CapabilityDispatchBuilder add(Capability<T> cap,ICapabilityProvider provider) {
		caps.put(cap, provider);
		return this;
	}
	public <T> CapabilityDispatchBuilder add(CapabilityStored<T> cap,ICapabilityProvider provider) {
		add(cap.capability(),provider);
		return this;
	}
	
	public <T> CapabilityDispatchBuilder add(TransientCapability<T> cap,NonNullSupplier<T> provider) {
		add(cap,cap.provider(provider));
		return this;
	}
	public <T,C extends ICapabilityProvider&CapabilityStored<T>> CapabilityDispatchBuilder add(C provider) {
		add(provider.capability(),provider);
		return this;
	}
	public CapabilityDispatcher build() {
		return new CapabilityDispatcher(caps);
	}
}
