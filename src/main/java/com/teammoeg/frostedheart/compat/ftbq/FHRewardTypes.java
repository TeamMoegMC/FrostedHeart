package com.teammoeg.frostedheart.compat.ftbq;

import com.teammoeg.frostedheart.FHMain;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;

public interface FHRewardTypes {

    RewardType INSIGHT = RewardTypes.register(FHMain.rl("insight"), InsightReward::new,
            () -> Icon.getIcon("frostedheart:item/quill_and_ink"));

    static void init() {
    }


}
