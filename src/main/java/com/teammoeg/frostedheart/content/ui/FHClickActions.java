package com.teammoeg.frostedheart.content.ui;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.ui.tips.TipHelper;

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
    }
}
