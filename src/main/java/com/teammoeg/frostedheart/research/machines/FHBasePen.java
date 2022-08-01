package com.teammoeg.frostedheart.research.machines;

import com.teammoeg.frostedheart.base.item.FHBaseItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class FHBasePen extends FHBaseItem implements IPen {

    public FHBasePen(String name, Properties properties) {
        super(name, properties);
    }

    @Override
    public void doDamage(PlayerEntity e, ItemStack stack, int val) {
        stack.damageItem(val, e, ex -> {
        });
    }

    @Override
    public boolean canUse(PlayerEntity e, ItemStack stack, int val) {
        return true;
    }

    @Override
    public int getLevel(ItemStack is, PlayerEntity player) {
        return 0;
    }

}
