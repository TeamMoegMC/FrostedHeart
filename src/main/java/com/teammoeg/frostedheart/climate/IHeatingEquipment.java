package com.teammoeg.frostedheart.climate;

import net.minecraft.item.ItemStack;

/**
 * Interface IHeatingEquipment.
 * Interface for Heating Equipment Item
 * @author khjxiaogu
 * file: IHeatingEquipment.java
 * @date 2021年9月14日
 */
public interface IHeatingEquipment {
	
	/**
	 * Compute new body temperature.<br>
	 *
	 * @param stack the stack<br>
	 * @param bodyTemp the body temp<br>
	 * @param environmentTemp the environment temp<br>
	 * @return returns new body temperature
	 */
	float compute(ItemStack stack,float bodyTemp,float environmentTemp);
}
