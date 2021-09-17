package com.teammoeg.frostedheart.climate;

import net.minecraft.item.ItemStack;

/**
 * Interface IHotFood.
 * Interface for body warming consumables
 * @author khjxiaogu
 * file: IHotFood.java
 * @date 2021年9月14日
 */
public interface ITempAdjustFood {
	
	/**
	 * Get max temperature this item can get.
	 *
	 * @param is the stack<br>
	 * @return max temp<br>
	 */
	default float getMaxTemp(ItemStack is) {
		return 15;
	};
	/**
	 * Get min temperature this item can get.
	 *
	 * @param is the stack<br>
	 * @return max temp<br>
	 */
	default float getMinTemp(ItemStack is) {
		return -15;
	};
	/**
	 * Get delta temperature this item would give.
	 *
	 * @param is the is<br>
	 * @return heat<br>
	 */
	float getHeat(ItemStack is);
}
