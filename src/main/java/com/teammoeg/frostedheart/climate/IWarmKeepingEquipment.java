package com.teammoeg.frostedheart.climate;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;

// TODO: Auto-generated Javadoc

/**
 * Interface IWarmKeepingEquipment.
 * Interface for warmkeeping equipments
 *
 * @author khjxiaogu
 * file: IWarmKeepingEquipment.java
 * @date 2021年9月14日
 */
public interface IWarmKeepingEquipment {

    /**
     * returns warm keeping factor.
     * max factor is 1.
     *
     * @param stack the stack<br>
     * @return factor<br>
     */
    float getFactor(ServerPlayerEntity pe, ItemStack stack);
}
