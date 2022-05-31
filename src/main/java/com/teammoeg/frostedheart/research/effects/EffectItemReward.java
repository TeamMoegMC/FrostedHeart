package com.teammoeg.frostedheart.research.effects;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Reward the research team item rewards
 */
public class EffectItemReward extends Effect {

    List<ItemStack> rewards;

    public EffectItemReward(ItemStack... stacks) {
    	super(GuiUtils.translateGui("effect.item_reward"),null,FHIcons.getIcon(stacks));
        rewards = new ArrayList<>();
        for (ItemStack stack : stacks) {
            rewards.add(stack);
        }
        if (rewards.size() != 0) {
            icon = FHIcons.getIcon(rewards);
        } else {
            icon = FHIcons.nop();
        }
        tooltip = new ArrayList<>();
        tooltip.add(GuiUtils.translateTooltip("effect.item_reward.1"));
    }
    public EffectItemReward(JsonObject jo) {
    	super(jo);
    }
    @Override
    public void init() {

    }

    public List<ItemStack> getRewards() {
        return rewards;
    }

    @Override
    public void grant(TeamResearchData team, PlayerEntity triggerPlayer) {
    	for(ItemStack s:rewards)
    		FHUtils.giveItem(triggerPlayer,s.copy());
    }
    //We dont confiscate players items, that is totally unnecessary
    @Override
    public void revoke(TeamResearchData team) {

    }

	@Override
	public String getId() {
		return "item";
	}
}
