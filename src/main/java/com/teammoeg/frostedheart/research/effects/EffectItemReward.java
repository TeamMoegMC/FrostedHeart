package com.teammoeg.frostedheart.research.effects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketBuffer;

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
        name = GuiUtils.translateGui("effect.itemreward");
        if (rewards.size() != 0) {
            icon = rewards.get(0);
        } else {
            icon = new ItemStack(Items.GRASS_BLOCK);
        }
        tooltip = new ArrayList<>();
        tooltip.add(GuiUtils.translateTooltip("effect.itemreward.1"));
    }
    public EffectItemReward(JsonObject jo) {}
    @Override
    public void init() {

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

	@Override
	public JsonElement serialize() {
		return null;
	}

	@Override
	public void write(PacketBuffer buffer) {
	}
}
