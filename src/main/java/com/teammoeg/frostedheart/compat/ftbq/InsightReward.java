/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.compat.ftbq;

import com.teammoeg.chorda.dataholders.team.TeamDataClosure;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.util.client.FHClientUtils;
import com.teammoeg.frostedheart.util.client.Lang;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbquests.net.DisplayRewardToastMessage;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class InsightReward extends Reward {
    private int insight;

    public InsightReward(long id, Quest quest, int insight) {
        super(id, quest);
        this.insight = insight;
    }

    public InsightReward(long id, Quest quest) {
        this(id, quest, 100);
    }

    @Override
    public RewardType getType() {
        return FHRewardTypes.INSIGHT;
    }

    @Override
    public void writeData(CompoundTag nbt) {
        super.writeData(nbt);
        nbt.putInt("insight", insight);
    }

    @Override
    public void readData(CompoundTag nbt) {
        super.readData(nbt);
        insight = nbt.getInt("insight");
    }

    @Override
    public void writeNetData(FriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeVarInt(insight);
    }

    @Override
    public void readNetData(FriendlyByteBuf buffer) {
        super.readNetData(buffer);
        insight = buffer.readVarInt();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addInt("insight", insight, v -> insight = v, 100, 1, Integer.MAX_VALUE).setNameKey(FHClientUtils.rawQuestReward("insight"));
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        TeamDataClosure<TeamResearchData> trd = ResearchDataAPI.getData(player);
        trd.get().addInsight(trd.team(),insight);
        if (notify) {
            MutableComponent message = Lang.questReward("insight").text(": +" + insight).style(ChatFormatting.GREEN).component();
            new DisplayRewardToastMessage(id, message, Color4I.empty()).sendTo(player);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public MutableComponent getAltTitle() {
        return Lang.questReward("insight").text(": +" + insight).style(ChatFormatting.GREEN).component();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public String getButtonText() {
        return "+" + insight;
    }
}
