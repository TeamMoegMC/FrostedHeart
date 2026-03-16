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

package com.teammoeg.chorda.listeners;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Chorda通用模组总线事件监听器，处理注册事件。
 * 在菜单类型注册时初始化自定义菜单槽位。
 * <p>
 * Chorda common mod bus event listener handling registration events.
 * Initializes custom menu slots during menu type registration.
 */
@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ChordaCommonEventsMod {
	/**
	 * 在注册事件中初始化自定义菜单槽位编码器。
	 * <p>
	 * Initializes custom menu slot encoders during the register event.
	 *
	 * @param event 注册事件 / The register event
	 */
	@SubscribeEvent
	public static void registerIngredient(RegisterEvent event) {
		if (event.getRegistryKey().equals(ForgeRegistries.Keys.MENU_TYPES)) {
			CCustomMenuSlot.init();
		}
	}
    
  
}
