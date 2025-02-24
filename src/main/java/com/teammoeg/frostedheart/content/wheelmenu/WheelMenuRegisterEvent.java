package com.teammoeg.frostedheart.content.wheelmenu;

import java.util.Map;

import com.teammoeg.chorda.util.CFunctionHelper;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

public class WheelMenuRegisterEvent extends Event{
	Map<ResourceLocation,Selection> toAdd;


	public WheelMenuRegisterEvent(Map<ResourceLocation, Selection> toAdd) {
		super();
		this.toAdd = toAdd;
	}


	public void register(ResourceLocation id,Selection selection) {
		toAdd.put(id, selection);
	}
}
