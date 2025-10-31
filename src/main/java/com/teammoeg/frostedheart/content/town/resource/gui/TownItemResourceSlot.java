package com.teammoeg.frostedheart.content.town.resource.gui;

import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.resource.action.ITownResourceActionExecutor;
import com.teammoeg.frostedheart.content.town.resource.action.ITownResourceActionExecutorHandler;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 这个格子并不真的存储物品，仅在玩家点击动作与城镇资源之间充当交互媒介
 */
public class TownItemResourceSlot extends Slot {
    //public final Town town;
    public final ITownResourceActionExecutorHandler executorHandler;
    public TownItemResourceSlot(Town town, Container pContainer/*无用，填入空值即可*/, int pSlot, int pX, int pY) {
        super(pContainer, pSlot, pX, pY);
        //this.town = town;
        this.executorHandler = town.getActionExecutorHandler();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return TownResourceActions.getCapacityLeft(executorHandler) >= 1.0F;
    }


}
