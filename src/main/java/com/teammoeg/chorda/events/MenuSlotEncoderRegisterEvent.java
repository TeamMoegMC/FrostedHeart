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
