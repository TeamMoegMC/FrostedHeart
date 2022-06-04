package com.teammoeg.frostedheart.research.effects;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

/**
 * Reward the research team item rewards
 */
public class EffectItemReward extends Effect {

    List<ItemStack> rewards;
    FHIcon iicons;
    public EffectItemReward(ItemStack... stacks) {
    	super("@gui." + FHMain.MODID + ".effect.item_reward",new ArrayList<>());
        rewards = new ArrayList<>();

        for (ItemStack stack : stacks) {
            rewards.add(stack);
            if(stack.getCount()==1)
            	tooltip.add("@"+stack.getTranslationKey());
            else
            	tooltip.add("{"+stack.getTranslationKey()+"} x "+stack.getCount());
        }
        initIcons();
    }
    private void initIcons() {
    	if (rewards.size() != 0) {
        	iicons = FHIcons.getIcon(rewards);
        } else {
        	iicons = FHIcons.nop();
        }
    }
    public EffectItemReward(JsonObject jo) {
    	super(jo);
    	rewards=SerializeUtil.parseJsonElmList(jo.get("rewards"),SerializeUtil::fromJson);
    	initIcons();
    }
    
    public EffectItemReward(PacketBuffer pb) {
		super(pb);
		rewards=SerializeUtil.readList(pb,PacketBuffer::readItemStack);
		initIcons();
	}
	@Override
    public void init() {

    }

    public List<ItemStack> getRewards() {
        return rewards;
    }

    @Override
    public boolean grant(TeamResearchData team, PlayerEntity triggerPlayer) {
    	if(triggerPlayer==null)return false;
    	for(ItemStack s:rewards)
    		FHUtils.giveItem(triggerPlayer,s.copy());
    	return true;
    }
    //We dont confiscate players items, that is totally unnecessary
    @Override
    public void revoke(TeamResearchData team) {

    }

	@Override
	public String getId() {
		return "item";
	}
	@Override
	public JsonObject serialize() {
		JsonObject jo=super.serialize();
		jo.add("rewards",SerializeUtil.toJsonList(rewards,SerializeUtil::toJson));
		return jo;
	}
	@Override
	public void write(PacketBuffer buffer) {
		super.write(buffer);
		SerializeUtil.writeList2(buffer,rewards,PacketBuffer::writeItemStack);
	}
	@Override
	public FHIcon getIcon() {
		if(super.getIcon()!=FHIcons.nop())
			return super.getIcon();
		return iicons;
	}
	@Override
	public int getIntID() {
		return 3;
	}
}
