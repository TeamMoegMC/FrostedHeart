package com.teammoeg.frostedheart.compat.ftbq;

import com.teammoeg.frostedheart.content.tips.ServerTipSender;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class TipReward extends Reward {
    private String tipId;

    public TipReward(long id, Quest q) {
        this(id, q, "");
    }

    public TipReward(long id, Quest q, String tipId) {
        super(id, q);
        this.tipId = tipId;
    }

    @Override
    public void writeData(CompoundTag nbt) {
        super.writeData(nbt);
        nbt.putString("fh_tip", tipId);
    }

    @Override
    public void readData(CompoundTag nbt) {
        super.readData(nbt);
        tipId = nbt.getString("fh_tip");
    }

    @Override
    public void writeNetData(FriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(tipId);
    }

    @Override
    public void readNetData(FriendlyByteBuf buffer) {
        super.readNetData(buffer);
        tipId = buffer.readUtf();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addString("fh_tip", tipId, id -> tipId = id, "");
    }

    @Override
    public Component getAltTitle() {
        return Component.translatable("tips.frostedheart." + tipId + ".title");
    }

    @Override
    public RewardType getType() {
        return FHRewardTypes.TIP;
    }

    @Override
    public void claim(ServerPlayer serverPlayer, boolean b) {
        ServerTipSender.sendGeneral(tipId, serverPlayer);
    }
}
