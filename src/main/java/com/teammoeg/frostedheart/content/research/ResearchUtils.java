package com.teammoeg.frostedheart.content.research;

import com.teammoeg.chorda.util.client.ClientUtils;
import com.teammoeg.frostedheart.content.research.gui.ResearchGui;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.IScreenWrapper;
import net.minecraft.client.gui.screens.Screen;

public class ResearchUtils {
    public static void refreshResearchGui() {
        Screen cur = ClientUtils.mc().screen;
        if (cur instanceof IScreenWrapper) {
            BaseScreen bs = ((IScreenWrapper) cur).getGui();
            if (bs instanceof ResearchGui) {
                bs.refreshWidgets();
            }
        }
        ClientUtils.mc().getLanguageManager();
    }
}
