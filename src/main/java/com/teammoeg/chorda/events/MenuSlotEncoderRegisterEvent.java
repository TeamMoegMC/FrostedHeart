package com.teammoeg.chorda.events;

import com.teammoeg.chorda.io.registry.IdRegistry;
import com.teammoeg.chorda.menu.CCustomMenuSlot.NetworkEncoder;

import lombok.Getter;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

public class MenuSlotEncoderRegisterEvent extends Event implements IModBusEvent{
	@Getter
	IdRegistry<NetworkEncoder<?>> registry;

	public MenuSlotEncoderRegisterEvent(IdRegistry<NetworkEncoder<?>> encoders) {
		registry=encoders;
	}
	public <T> NetworkEncoder<T> register(NetworkEncoder<T> encoder) {
		return registry.register(encoder);
	}

}
