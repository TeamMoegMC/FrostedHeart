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
