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

package com.teammoeg.frostedheart.content.wheelmenu;

import java.util.Map;

import com.teammoeg.chorda.util.CFunctionUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

public class WheelMenuSelectionRegisterEvent extends Event{
	Map<ResourceLocation,Selection> toAdd;


	public WheelMenuSelectionRegisterEvent(Map<ResourceLocation, Selection> toAdd) {
		super();
		this.toAdd = toAdd;
	}


	public void register(ResourceLocation id,Selection selection) {
		toAdd.put(id, selection);
	}
}
