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

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;

public interface FHRewardTypes {

    RewardType INSIGHT = RewardTypes.register(FHMain.rl("insight"), InsightReward::new,
            () -> Icon.getIcon("frostedheart:item/quill_and_ink"));
    RewardType TEMPERATURE_DIFFICULTY = RewardTypes.register(FHMain.rl("temperature_difficulty"), TemperatureDifficultyReward::new,
            () -> Icon.getIcon("frostedheart:item/mercury_body_thermometer"));

    static void init() {
    }


}
