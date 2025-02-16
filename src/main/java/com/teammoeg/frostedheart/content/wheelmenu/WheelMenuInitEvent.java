package com.teammoeg.frostedheart.content.wheelmenu;

import java.util.function.Consumer;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class WheelMenuInitEvent extends Event {
	Consumer<Selection> adder;

	WheelMenuInitEvent(Consumer<Selection> adder) {
		super();
		this.adder = adder;
	}
	public void addSelection(Selection toadd) {
		adder.accept(toadd);
	}
}
