package com.teammoeg.frostedheart.research.effects;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.SerializeUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Reward the research team item rewards
 */
public class EffectItemReward extends Effect {

    List<ItemStack> rewards;

    public EffectItemReward(ItemStack... stacks) {
        super();
        rewards = new ArrayList<>();

        for (ItemStack stack : stacks) {
            rewards.add(stack);
        }
    }

    public EffectItemReward(JsonObject jo) {
        super(jo);
        rewards = SerializeUtil.parseJsonElmList(jo.get("rewards"), SerializeUtil::fromJson);
    }

    public EffectItemReward(PacketBuffer pb) {
        super(pb);
        rewards = SerializeUtil.readList(pb, PacketBuffer::readItemStack);
    }

    @Override
    public void init() {

    }

    public List<ItemStack> getRewards() {
        return rewards;
    }

    @Override
    public boolean grant(TeamResearchData team, PlayerEntity triggerPlayer, boolean isload) {
        if (triggerPlayer == null || isload) return false;
        for (ItemStack s : rewards) {
            FHUtils.giveItem(triggerPlayer, s.copy());

        }
        return true;
    }

    //We dont confiscate players items, that is totally unnecessary
    @Override
    public void revoke(TeamResearchData team) {

    }



    @Override
    public JsonObject serialize() {
        JsonObject jo = super.serialize();
        jo.add("rewards", SerializeUtil.toJsonList(rewards, SerializeUtil::toJson));
        return jo;
    }

    @Override
    public void write(PacketBuffer buffer) {
        super.write(buffer);
        SerializeUtil.writeList2(buffer, rewards, PacketBuffer::writeItemStack);
    }

    @Override
    public FHIcon getDefaultIcon() {
        if (rewards.size() != 0) {
            return FHIcons.getStackIcons(rewards);
        }
        return FHIcons.nop();
    }

    @Override
    public IFormattableTextComponent getDefaultName() {
        return GuiUtils.translateGui("effect.item_reward");
    }

    @Override
    public List<ITextComponent> getDefaultTooltip() {
        List<ITextComponent> tooltip = new ArrayList<>();
        for (ItemStack stack : rewards) {
            if (stack.getCount() == 1)
                tooltip.add(stack.getDisplayName());
            else
                tooltip.add(((IFormattableTextComponent) stack.getDisplayName()).appendSibling(new StringTextComponent(" x " + stack.getCount())));
        }
        return tooltip;
    }

    @Override
    public String getBrief() {
        if (rewards.isEmpty())
            return "Reward nothing";

        return "Reward " + rewards.get(0).getDisplayName().getString() + (rewards.size() > 1 ? " ..." : "");
    }
}
