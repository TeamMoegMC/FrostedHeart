package com.teammoeg.frostedheart.content.health.screen;

import com.teammoeg.chorda.menu.CBaseMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.climate.player.BodyPartData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Map;

public class HealthStatMenu extends CBaseMenu {
	public CDataSlot<Float> fat=CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> protein=CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> carbohydrate=CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> vegetable=CCustomMenuSlot.SLOT_FIXED.create(this);

	public CDataSlot<Float> headTemperature=CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> bodyTemperature=CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> handsTemperature=CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> legsTemperature=CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> feetTemperature=CCustomMenuSlot.SLOT_FIXED.create(this);



	public HealthStatMenu(int pContainerId, Inventory inventoryPlayer, FriendlyByteBuf extraData) {
		super(FHMenuTypes.NUTRITION_GUI.get(), pContainerId,inventoryPlayer.player, 0);
	}
	public HealthStatMenu(int pContainerId, Inventory inventoryPlayer) {
		super(FHMenuTypes.NUTRITION_GUI.get(), pContainerId,inventoryPlayer.player, 0);
		
		LazyOptional<NutritionCapability> nut_lo=NutritionCapability.getCapability(inventoryPlayer.player);
		nut_lo.ifPresent(cap->{
			fat.bind(()->Math.min(cap.get().getFat()/10000,1));
			protein.bind(()->Math.min(cap.get().getProtein()/10000,1));
			carbohydrate.bind(()->Math.min(cap.get().getCarbohydrate()/10000,1));
			vegetable.bind(()->Math.min(cap.get().getVegetable()/10000,1));
		});

		LazyOptional<PlayerTemperatureData> temp_lo = PlayerTemperatureData.getCapability(inventoryPlayer.player);
		temp_lo.ifPresent(cap->{
			cap.clothesOfParts.forEach((part, data)->{
				switch (part) {
					case HEAD:
						headTemperature.bind(()->data.getTemperature());
						break;
					case TORSO:
						bodyTemperature.bind(()->data.getTemperature());
						break;
					case HANDS:
						handsTemperature.bind(()->data.getTemperature());
						break;
					case LEGS:
						legsTemperature.bind(()->data.getTemperature());
						break;
					case FEET:
						feetTemperature.bind(()->data.getTemperature());
						break;
				}
			});

		});
	}

}
