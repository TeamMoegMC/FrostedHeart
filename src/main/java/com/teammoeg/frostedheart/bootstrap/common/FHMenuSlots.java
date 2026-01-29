package com.teammoeg.frostedheart.bootstrap.common;

import java.util.BitSet;

import com.teammoeg.chorda.events.MenuSlotEncoderRegisterEvent;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.Encoders;
import com.teammoeg.chorda.menu.CCustomMenuSlot.NetworkEncoder;
import com.teammoeg.chorda.menu.CCustomMenuSlot.OtherDataSlotEncoder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.robotics.logistics.Filter;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FHMenuSlots {
	public static final NetworkEncoder<Filter> FILTER_ENCODER=CCustomMenuSlot.Encoders.codec(Filter.CODEC);
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
	public FHMenuSlots() {
	}
	
	@SubscribeEvent
	public static void registerMenuSlot(MenuSlotEncoderRegisterEvent event) {
		event.getRegistry().register(FILTER_ENCODER);
	}
}
