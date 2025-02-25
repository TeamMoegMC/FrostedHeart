package com.teammoeg.frostedheart.content.wheelmenu;

import java.util.Map;

import com.teammoeg.chorda.util.CFunctionHelper;

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
