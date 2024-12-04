package com.teammoeg.frostedheart.compat.ftbq;

import dev.ftb.mods.ftblibrary.config.IntConfig;
import dev.ftb.mods.ftblibrary.config.ui.EditStringConfigOverlay;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import dev.ftb.mods.ftbquests.quest.reward.XPReward;

public class FHGuiProviders {
    public static void setRewardGuiProviders() {
        FHRewardTypes.INSIGHT.setGuiProvider((panel, quest, callback) -> {
            IntConfig c = new IntConfig(1, Integer.MAX_VALUE);
            c.setValue(100);

            EditStringConfigOverlay<Integer> overlay = new EditStringConfigOverlay<>(panel.getGui(), c, accepted -> {
                if (accepted) {
                    callback.accept(new InsightReward(0L, quest, c.getValue()));
                }

                panel.run();
            }, FHRewardTypes.INSIGHT.getDisplayName()).atMousePosition();
            panel.getGui().pushModalPanel(overlay);
        });
    }
}
