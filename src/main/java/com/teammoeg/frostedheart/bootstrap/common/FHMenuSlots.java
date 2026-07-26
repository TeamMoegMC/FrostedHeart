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

package com.teammoeg.frostedheart.bootstrap.common;

import java.util.BitSet;

import com.teammoeg.chorda.events.MenuSlotEncoderRegisterEvent;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.Encoders;
import com.teammoeg.chorda.menu.CCustomMenuSlot.NetworkEncoder;
import com.teammoeg.chorda.menu.CCustomMenuSlot.OtherDataSlotEncoder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.robotics.logistics.Filter;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseInterfaceTarget;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FHMenuSlots {
	public static final NetworkEncoder<Filter> FILTER_ENCODER=CCustomMenuSlot.Encoders.codec(Filter.CODEC);
	public static final NetworkEncoder<WarehouseInterfaceTarget> WAREHOUSE_INTERFACE_TARGET_ENCODER =
			CCustomMenuSlot.Encoders.codec(WarehouseInterfaceTarget.CODEC);
	public static final OtherDataSlotEncoder<Filter> FILTER_ENCODER_SLOT=new OtherDataSlotEncoder<>(){

		@Override
		public Filter copy(Filter data) {
			if(data==null)
				return null;
			return new Filter(data.getKey(),data.isIgnoreNbt(),data.getSize());
		}

		@Override
		public Filter getDefault() {
			return null;
		}

		@Override
		public NetworkEncoder<Filter> getEncoder() {
			return FILTER_ENCODER;
		}

	};
	public static final OtherDataSlotEncoder<WarehouseInterfaceTarget> WAREHOUSE_INTERFACE_TARGET_ENCODER_SLOT =
			new OtherDataSlotEncoder<>() {
				@Override
				public WarehouseInterfaceTarget copy(WarehouseInterfaceTarget data) {
					return data;
				}

				@Override
				public WarehouseInterfaceTarget getDefault() {
					return null;
				}

				@Override
				public NetworkEncoder<WarehouseInterfaceTarget> getEncoder() {
					return WAREHOUSE_INTERFACE_TARGET_ENCODER;
				}
			};
	public FHMenuSlots() {
	}
	
	@SubscribeEvent
	public static void registerMenuSlot(MenuSlotEncoderRegisterEvent event) {
		event.getRegistry().register(FILTER_ENCODER);
		event.getRegistry().register(WAREHOUSE_INTERFACE_TARGET_ENCODER);
	}
}
