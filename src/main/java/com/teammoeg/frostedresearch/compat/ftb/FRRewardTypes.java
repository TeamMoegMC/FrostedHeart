package com.teammoeg.frostedresearch.compat.ftb;

import com.teammoeg.frostedheart.FHMain;

import com.teammoeg.frostedresearch.FRMain;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;

public class FRRewardTypes {
    static RewardType INSIGHT = RewardTypes.register(FHMain.rl("insight"), InsightReward::new,
        () -> Icon.getIcon("frostedresearch:item/quill_and_ink"));
	public FRRewardTypes() {
	}
	public static void init() {}
}
