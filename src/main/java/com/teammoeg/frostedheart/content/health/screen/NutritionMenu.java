package com.teammoeg.frostedheart.content.health.screen;

import com.teammoeg.chorda.menu.CBaseMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.common.util.LazyOptional;

public class NutritionMenu extends CBaseMenu {
	public CDataSlot<Float> fat=CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> protein=CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> carbohydrate=CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> vegetable=CCustomMenuSlot.SLOT_FIXED.create(this);
	public NutritionMenu(int pContainerId, Inventory inventoryPlayer, FriendlyByteBuf extraData) {
		super(FHMenuTypes.NUTRITION_GUI.get(), pContainerId,inventoryPlayer.player, 0);
	}
	public NutritionMenu(int pContainerId, Inventory inventoryPlayer) {
		super(FHMenuTypes.NUTRITION_GUI.get(), pContainerId,inventoryPlayer.player, 0);
		
		LazyOptional<NutritionCapability> lo=NutritionCapability.getCapability(inventoryPlayer.player);
		lo.ifPresent(cap->{
			fat.bind(()->cap.get().getFat()/10000);
			protein.bind(()->cap.get().getProtein()/10000);
			carbohydrate.bind(()->cap.get().getCarbohydrate()/10000);
			vegetable.bind(()->cap.get().getVegetable()/10000);
		});
		
	}

}
