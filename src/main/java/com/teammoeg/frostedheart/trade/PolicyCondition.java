package com.teammoeg.frostedheart.trade;

import com.teammoeg.frostedheart.util.Writeable;

import net.minecraft.entity.merchant.villager.VillagerEntity;

public interface PolicyCondition extends Writeable{
	boolean test(FHVillagerData ve);
}
