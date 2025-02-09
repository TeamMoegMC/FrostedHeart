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

package com.teammoeg.chorda.capability.types.nonpresistent;

import com.teammoeg.chorda.capability.CapabilityStored;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;

public class TransientCapabilityProvider<C> implements ICapabilityProvider,CapabilityStored<C>{
	LazyOptional<C> lazyCap;
	TransientCapability<C> capability;
	public TransientCapabilityProvider(TransientCapability<C> capability, NonNullSupplier<C> factory) {
		super();
		this.capability = capability;
		this.lazyCap=LazyOptional.of(factory);
	}
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if(cap==capability.capability()) {
			return lazyCap.cast();
		}
		return LazyOptional.empty();
	}
	@Override
	public Capability<C> capability() {
		return capability.capability();
	}


}
