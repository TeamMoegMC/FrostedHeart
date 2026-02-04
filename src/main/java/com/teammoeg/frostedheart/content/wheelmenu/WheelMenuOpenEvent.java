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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
/**
 * Fired when player attempts to open wheel menu, canceling this to keep player from using wheel menu
 * You may reorder or
 * */
@Cancelable
public class WheelMenuOpenEvent extends Event {
	List<ResourceLocation> toShow;
	public WheelMenuOpenEvent(List<ResourceLocation> toShow) {
		super();
		this.toShow = toShow;
	}

	public void showFirst(ResourceLocation id) {
		toShow.add(0,id);
	}
	public void showLast(ResourceLocation id) {
		toShow.add(id);
	}
	public void showBefore(ResourceLocation id,ResourceLocation beforeWhich) {
		hide(id);
		int idx=toShow.indexOf(beforeWhich);
		if(idx>=0) {
			toShow.add(idx,id);
		}else
			toShow.add(0,id);
	}
	public void showAfter(ResourceLocation id,ResourceLocation beforeWhich) {
		hide(id);
		int idx=toShow.indexOf(beforeWhich);
		if(idx<0) {
			toShow.add(idx+1,id);
		}else
			toShow.add(id);
	}
	public boolean hide(ResourceLocation id) {
		return toShow.remove(id);
	}
	public boolean contains(ResourceLocation id) {
		return toShow.contains(id);
	}
}
