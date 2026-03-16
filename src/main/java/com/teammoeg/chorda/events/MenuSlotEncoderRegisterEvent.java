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

package com.teammoeg.chorda.events;

import com.teammoeg.chorda.io.registry.IdRegistry;
import com.teammoeg.chorda.menu.CCustomMenuSlot.NetworkEncoder;

import lombok.Getter;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

/**
 * 自定义菜单槽位网络编码器的注册事件，在模组总线上触发。
 * 模组可以监听此事件来注册自定义的菜单槽位网络编码器。
 * <p>
 * Registration event for custom menu slot network encoders, fired on the mod bus.
 * Mods can listen to this event to register custom menu slot network encoders.
 */
public class MenuSlotEncoderRegisterEvent extends Event implements IModBusEvent{
	@Getter
	IdRegistry<NetworkEncoder<?>> registry;

	/**
	 * 创建菜单槽位编码器注册事件。
	 * <p>
	 * Creates a menu slot encoder register event.
	 *
	 * @param encoders 编码器注册表 / The encoder registry
	 */
	public MenuSlotEncoderRegisterEvent(IdRegistry<NetworkEncoder<?>> encoders) {
		registry=encoders;
	}
	/**
	 * 注册一个网络编码器到注册表中。
	 * <p>
	 * Registers a network encoder to the registry.
	 *
	 * @param encoder 要注册的编码器 / The encoder to register
	 * @param <T> 编码器处理的数据类型 / The data type handled by the encoder
	 * @return 注册后的编码器 / The registered encoder
	 */
	public <T> NetworkEncoder<T> register(NetworkEncoder<T> encoder) {
		return registry.register(encoder);
	}

}
