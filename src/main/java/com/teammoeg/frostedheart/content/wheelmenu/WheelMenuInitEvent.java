package com.teammoeg.frostedheart.content.wheelmenu;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class WheelMenuInitEvent extends Event {
	List<ResourceLocation> toShow;
	public WheelMenuInitEvent(List<ResourceLocation> toShow) {
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
