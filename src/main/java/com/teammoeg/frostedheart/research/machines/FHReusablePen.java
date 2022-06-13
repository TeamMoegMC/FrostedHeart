package com.teammoeg.frostedheart.research.machines;

import com.teammoeg.frostedheart.base.item.FHBaseItem;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class FHReusablePen extends FHBaseItem implements IPen {
	int lvl;
	public FHReusablePen(String name, Properties properties,int lvl) {
		super(name, properties);
		this.lvl=lvl;
	}

	@Override
	public void doDamage(PlayerEntity e,ItemStack stack,int val) {
		stack.damageItem(val,e, ex->{});
	}

	@Override
	public boolean canUse(PlayerEntity e,ItemStack stack,int val) {
		return stack.getDamage()<stack.getMaxDamage()-val;
	}

	@Override
	public int getLevel(ItemStack is, PlayerEntity player) {
		return lvl;
	}

}
