package com.teammoeg.frostedheart.content.tips;

import com.teammoeg.chorda.CompatModule;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.FTBQuestsClient;
import net.minecraft.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class TipClickActions {
    private static final Map<String, Consumer<String>> CLICK_ACTIONS = new HashMap<>();

    static {
        register("OpenURL", s -> Util.getPlatform().openUri(s));
        register("OpenQuest", s -> {
            if (CompatModule.isFTBQLoaded()) {
                if (FTBQuestsClient.getClientQuestFile() != null) {
                    var quest = FTBQuestsClient.getClientQuestFile().getQuest(Long.parseLong(s, 16));
                    ClientQuestFile.openGui(quest, true);
                }
            }
        });
    }

    public static boolean hasAction(String name) {
        return CLICK_ACTIONS.containsKey(name);
    }

    public static void run(String name, String context) {
        var runner = CLICK_ACTIONS.get(name);
        if (runner != null) {
            runner.accept(context);
        } else {
            Tip.LOGGER.warn("Unknown click action name '{}', accepted context: '{}'", name, context);
        }
    }

    public static Set<String> getActionKeys() {
        return CLICK_ACTIONS.keySet();
    }

    public static Consumer<String> getAction(String key) {
        return CLICK_ACTIONS.get(key);
    }

    public static void register(String actionName, Consumer<String> runner) {
        CLICK_ACTIONS.put(actionName, runner);
    }
}
