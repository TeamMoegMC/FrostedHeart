package com.teammoeg.frostedheart.content.ui;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.ui.tips.TipHelper;
import com.teammoeg.frostedheart.content.ui.tips.client.gui.TipOverlay;
import com.teammoeg.frostedheart.item.townmanager.TownManagerClientHelper;

import static com.teammoeg.chorda.client.ClickActions.register;

public class FHClickActions {
    public static void init() {
        register(FHMain.rl("edit_tip"),        "tips.frostedheart.click_action.edit_tip", s -> {
            if (s.startsWith("{")) {
                TipHelper.edit(TipHelper.parse(s), null);
            } else {
                TipHelper.edit(s, null);
            }
        });
        register(FHMain.rl("open_town_events"),
                "tips.frostedheart.click_action.open_town_events", ignored -> {
                    TipOverlay.removeCurrent();
                    TownManagerClientHelper.openEvents();
                });
        register(FHMain.rl("open_town_transport"),
                "tips.frostedheart.click_action.open_town_transport", ignored -> {
                    TipOverlay.removeCurrent();
                    TownManagerClientHelper.openTransportCapacity();
                });
    }
}
