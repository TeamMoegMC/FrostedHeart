package com.teammoeg.chorda.compat.ftb;

import com.teammoeg.chorda.CompatModule;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.FTBQuestsClient;

public class FTBQCompat {
    public static void openQuest(long id) {
        if (CompatModule.isFTBQLoaded()) {
            if (FTBQuestsClient.getClientQuestFile() != null) {
                var quest = FTBQuestsClient.getClientQuestFile().getQuest(id);
                ClientQuestFile.openGui(quest, true);
            }
        }
    }
}
