package com.teammoeg.frostedheart.content.tips;

import com.mojang.logging.LogUtils;
import com.teammoeg.chorda.CompatModule;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.content.archive.ArchiveScreen;
import com.teammoeg.frostedresearch.compat.ftb.FTBQCompat;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ClickActions {
    private static final Map<String, Action> ACTIONS = new HashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Action NO_ACTION = new Action("NoAction", "tips.frostedheart.click_action.empty", s -> {});

    static {
        registerActions();
    }

    private static void registerActions() {
        ACTIONS.clear();
        register(NO_ACTION);
        register("OpenURL",       "tips.frostedheart.click_action.open_url",     s -> {
            try {
                Util.getPlatform().openUrl(new URL(s));
            } catch (MalformedURLException e) {
                throw new RuntimeException("Invalid URL '" + s + "'");
            }
        });
        register("OpenQuest",     "tips.frostedheart.click_action.open_quest",   s -> {
            if (!CompatModule.isFTBQLoaded()) {
                throw new RuntimeException("FTBQuest is not installed");
            }
            FTBQCompat.openGui(Long.parseLong(s, 16));
        });
        register("OpenFile",      "tips.frostedheart.click_action.open_file",    s -> {
            var file = new File(FMLPaths.GAMEDIR.get().toFile(), s);
            if (!file.exists()) {
                throw new RuntimeException("File '" + file + "' not exists");
            }
            Util.getPlatform().openUri(file.toURI());
        });
        register("ShowJeiRecipe", "tips.frostedheart.click_action.jei_recipe",   s -> {
            var item = CRegistryHelper.getItem(ResourceLocation.tryParse(s)).getDefaultInstance();
            if (item.isEmpty()) {
                throw new RuntimeException("Unknown Item '%s'".formatted(s));
            }
            JEICompat.showJEIFor(item);
        });
        register("ShowJeiUsages", "tips.frostedheart.click_action.jei_usages",   s -> {
            var item = CRegistryHelper.getItem(ResourceLocation.tryParse(s)).getDefaultInstance();
            if (item.isEmpty()) {
                throw new RuntimeException("Unknown Item '%s'".formatted(s));
            }
            JEICompat.showJEIUsageFor(item);
        });
        register("ViewInArchive", "tips.frostedheart.click_action.open_archive", ArchiveScreen::open);
    }

    public static void register(String actionName, String description, Consumer<String> action) {
        register(new Action(actionName, description, action));
    }

    private static void register(Action action) {
        ACTIONS.put(action.name, action);
    }

    public static boolean run(String actionName, String context) {
        return run(getByName(actionName), context);
    }

    private static boolean run(Action action, String context) {
        String message;
        try {
            if (action != NO_ACTION) {
                action.run(context);
                return true;
            }
            message = "Unknown action";
        } catch (Throwable e) {
            message = "An error occurred while running action '%s', accepted context: '%s'".formatted(action.name, context);
            LOGGER.error(message, e);
        }
        Popup.put(Components.withColor(message, Colors.RED).append("\n").append(Component.translatable("tips.frostedheart.error.desc")));
        return false;
    }

    public static Component getDesc(String actionName, Object... args) {
        var action = getByName(actionName);
        return Component.translatable(action.description, args);
    }

    private static Action getByName(String actionName) {
        var action = ACTIONS.get(actionName);
        if (action == null) {
            LOGGER.warn("Unknown action '{}'", actionName);
            return NO_ACTION;
        }
        return action;
    }

    public static List<String> getAllNames() {
        return new ArrayList<>(ACTIONS.keySet());
    }

    public static boolean hasAction(String actionName) {
        return ACTIONS.containsKey(actionName);
    }

    record Action(String name, String description, Consumer<String> action) {
        void run(String context) {
            action.accept(context);
        }
    }
}
