package com.teammoeg.frostedheart.climate;

import net.minecraft.item.ItemStack;

/**
 * Interface IHotFood.
 * Interface for body warming consumables
 * @author khjxiaogu
 * file: IHotFood.java
 * @date 2021年9月14日
 */
public interface IHotFood {
	
	/**
	 * Get max temperature this item can get.
	 *
	 * @param is the stack<br>
	 * @return max temp<br>
	 */
	float getMaxTemp(ItemStack is);
	
	/**
	 * Get delta temperature this item would give.
	 *
	 * @param is the is<br>
	 * @return heat<br>
	 */
	float getHeat(ItemStack is);
}
