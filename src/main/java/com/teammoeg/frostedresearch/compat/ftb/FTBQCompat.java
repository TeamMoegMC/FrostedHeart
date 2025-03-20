package com.teammoeg.frostedresearch.compat.ftb;

import dev.ftb.mods.ftblibrary.config.IntConfig;
import dev.ftb.mods.ftblibrary.config.ui.EditStringConfigOverlay;

public class FTBQCompat {

	public FTBQCompat() {
	}
	public static void setRewardGuiProviders() {
        FRRewardTypes.INSIGHT.setGuiProvider((panel, quest, callback) -> {
            IntConfig c = new IntConfig(1, Integer.MAX_VALUE);
            c.setValue(100);

            EditStringConfigOverlay<Integer> overlay = new EditStringConfigOverlay<>(panel.getGui(), c, accepted -> {
                if (accepted) {
                    callback.accept(new InsightReward(0L, quest, c.getValue()));
                }

                panel.run();
            }, FRRewardTypes.INSIGHT.getDisplayName()).atMousePosition();
            panel.getGui().pushModalPanel(overlay);
        });
    }
}
