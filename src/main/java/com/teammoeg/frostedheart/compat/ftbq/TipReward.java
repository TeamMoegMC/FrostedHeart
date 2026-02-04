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
