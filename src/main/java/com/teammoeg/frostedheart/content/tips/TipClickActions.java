package com.teammoeg.frostedheart.content.tips;

import net.minecraft.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TipClickActions {
    private static final Map<String, Consumer<String>> CLICK_ACTIONS = new HashMap<>();

    static {
        register("OpenURL", s -> Util.getPlatform().openUri(s));
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

    public static void register(String actionName, Consumer<String> runner) {
        CLICK_ACTIONS.put(actionName, runner);
    }
}
