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

import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.util.client.Lang;
import com.teammoeg.chorda.client.icon.CIconFTBWrapper;
import com.teammoeg.frostedheart.content.climate.FHTemperatureDifficulty;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
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

import java.util.List;

public class TemperatureDifficultyReward extends Reward {
    private FHTemperatureDifficulty temperatureDifficulty;

    public TemperatureDifficultyReward(long id, Quest quest, FHTemperatureDifficulty temperatureDifficulty) {
        super(id, quest);
        this.temperatureDifficulty = temperatureDifficulty;
    }

    public TemperatureDifficultyReward(long id, Quest quest) {
        this(id, quest, FHTemperatureDifficulty.normal);
    }

    @Override
    public RewardType getType() {
        return FHRewardTypes.TEMPERATURE_DIFFICULTY;
    }

    @Override
    public void writeData(CompoundTag nbt) {
        super.writeData(nbt);
        // Write temperature difficulty
        nbt.putString("temperature_difficulty", temperatureDifficulty.name());
    }

    @Override
    public void readData(CompoundTag nbt) {
        super.readData(nbt);
        // Read temperature difficulty
        temperatureDifficulty = FHTemperatureDifficulty.valueOf(nbt.getString("temperature_difficulty"));
    }

    @Override
    public void writeNetData(FriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        // write temperature difficulty string
        buffer.writeUtf(temperatureDifficulty.name());
    }

    @Override
    public void readNetData(FriendlyByteBuf buffer) {
        super.readNetData(buffer);
        // read temperature difficulty string
        temperatureDifficulty = FHTemperatureDifficulty.valueOf(buffer.readUtf());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        // list values
        List<FHTemperatureDifficulty> difficulties = List.of(FHTemperatureDifficulty.values());

        config.addEnum("temperature_difficulty",
                temperatureDifficulty,
                v -> temperatureDifficulty = v,
                NameMap.of(FHTemperatureDifficulty.normal, difficulties)
                        .nameKey(v -> "temperature_difficulty.frostedheart." + v.name())
                        .icon(v -> new CIconFTBWrapper(v.icon))
                        .create(),
                FHTemperatureDifficulty.normal
        );

    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        PlayerTemperatureData.getCapability(player).ifPresent(data -> {
            data.setDifficulty(temperatureDifficulty);
        });
        if (notify) {
            new DisplayRewardToastMessage(id, Lang.questReward("insight").text(": +" + temperatureDifficulty.name()).style(ChatFormatting.GREEN).component(), Color4I.empty()).sendTo(player);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public MutableComponent getAltTitle() {
        return Lang.questReward("insight").text(": +" + temperatureDifficulty.name()).style(ChatFormatting.GREEN).component();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Icon getAltIcon() {
        return new CIconFTBWrapper(temperatureDifficulty.icon);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public String getButtonText() {
        return temperatureDifficulty.name();
    }
}
