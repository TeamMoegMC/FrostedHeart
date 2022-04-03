package com.teammoeg.frostedheart.research.effects;

import com.teammoeg.frostedheart.client.util.GuiUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Reward the research team item rewards
 */
public class EffectItemReward extends Effect {

    List<ItemStack> rewards;

    public EffectItemReward(ItemStack... stacks) {
        rewards = new ArrayList<>();
        for (ItemStack stack : stacks) {
            rewards.add(stack);
        }
    }

    @Override
    public void init() {
        name = GuiUtils.translateGui("effect.itemreward");
        if (rewards.size() != 0) {
            icon = rewards.get(0);
        } else {
            icon = new ItemStack(Items.GRASS_BLOCK);
        }
        tooltip = new ArrayList<>();
        tooltip.add(GuiUtils.translateTooltip("effect.itemreward.1"));
    }

    public List<ItemStack> getRewards() {
        return rewards;
    }

    @Override
    public void grant() {

    }

    @Override
    public void revoke() {

    }
}
