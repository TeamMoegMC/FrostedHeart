/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.tips;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.CompatModule;
import com.teammoeg.chorda.client.cui.editor.Editor;
import com.teammoeg.chorda.client.cui.editor.EditorDialogBuilder;
import com.teammoeg.chorda.client.cui.editor.Editors;
import com.teammoeg.chorda.math.Colors;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedresearch.compat.ftb.FTBQCompat;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.teammoeg.frostedheart.FHMain.LOGGER;

public class ClickActions {
    private static final Map<ResourceLocation, Action> ACTIONS = new HashMap<>();
    private static final Action EMPTY = new Action(FHMain.rl("empty"), "tips.frostedheart.click_action.empty", s -> {});
    public static final ClickAction NO_ACTION = new ClickAction(FHMain.rl("empty"), "");

    public static final Codec<ClickAction> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("clickAction").forGetter(ClickAction::action),
            Codec.STRING.fieldOf("clickActionContent").forGetter(ClickAction::content)
    ).apply(i, ClickAction::new));
    public static final Editor<ClickAction> EDITOR = EditorDialogBuilder.create(b -> b
            .add(Editors.RESOURCELOCATION.withName(Component.literal("Action Name")).forGetter(ClickAction::action))
            .add(Editors.STRING.withName(Component.literal("Context")).forGetter(ClickAction::content))
            .apply(ClickAction::new)
    );
    public record ClickAction(ResourceLocation action, String content) {
        public void run() {
            ClickActions.run(ClickActions.get(action), content);
        }

        public MutableComponent getDesc() {
            return Component.translatable(ClickActions.get(action).description, content);
        }
    }
    public static ClickAction create(ResourceLocation action, String content) {
        return new ClickAction(action, content);
    }

    static {
        register(EMPTY);
        register(FHMain.rl("open_url"),        "tips.frostedheart.click_action.open_url",   s -> {
            try {
                Util.getPlatform().openUrl(new URL(s));
            } catch (MalformedURLException e) {
                throw new RuntimeException("Invalid URL '" + s + "'");
            }
        });
        register(FHMain.rl("open_quest"),      "tips.frostedheart.click_action.open_quest", s -> {
            if (!CompatModule.isFTBQLoaded()) {
                throw new RuntimeException("FTBQuest is not installed");
            }
            FTBQCompat.openGui(Long.parseLong(s, 16));
        });
        register(FHMain.rl("open_file"),       "tips.frostedheart.click_action.open_file",  s -> {
            var file = new File(FMLPaths.GAMEDIR.get().toFile(), s);
            if (!file.exists()) {
                throw new RuntimeException("File '" + file + "' not exists");
            }
            Util.getPlatform().openUri(file.toURI());
        });
        register(FHMain.rl("show_jei_recipe"), "tips.frostedheart.click_action.jei_recipe", s -> {
            var item = CRegistryHelper.getItem(ResourceLocation.tryParse(s)).getDefaultInstance();
            if (item.isEmpty()) {
                throw new RuntimeException("Unknown Item '%s'".formatted(s));
            }
            JEICompat.showJEIFor(item);
        });
        register(FHMain.rl("show_jei_usages"), "tips.frostedheart.click_action.jei_usages", s -> {
            var item = CRegistryHelper.getItem(ResourceLocation.tryParse(s)).getDefaultInstance();
            if (item.isEmpty()) {
                throw new RuntimeException("Unknown Item '%s'".formatted(s));
            }
            JEICompat.showJEIUsageFor(item);
        });
        register(FHMain.rl("edit_tip"),        "tips.frostedheart.click_action.edit_tip",   s -> {
            if (s.startsWith("{")) {
                TipHelper.edit(TipHelper.parse(s), null);
            } else {
                TipHelper.edit(s, null);
            }
        });
    }
    private ClickActions() {}

    public static void register(ResourceLocation location, String description, Consumer<String> action) {
        register(new Action(location, description, action));
    }

    private static Action register(Action action) {
        ACTIONS.put(action.location, action);
        return action;
    }

    public static boolean run(ResourceLocation loc, String context) {
        return run(get(loc), context);
    }

    private static boolean run(Action action, String context) {
        String message;
        try {
            if (action != EMPTY) {
                action.run(context);
                return true;
            }
            message = "Unknown action";
        } catch (Throwable e) {
            message = "Unable to execute action '%s', accepted context: '%s'".formatted(action.location, context);
            LOGGER.error(message, e);
        }
        Popup.put(Components.withColor(message, Colors.RED).append("\n").append(Component.translatable("tips.frostedheart.error.desc")));
        return false;
    }

    public static Set<ResourceLocation> getAllRLs() {
        return new HashSet<>(ACTIONS.keySet());
    }

    public static boolean hasAction(ResourceLocation location) {
        return get(location) != EMPTY;
    }

    private static Action get(ResourceLocation location) {
        for (Action action : ACTIONS.values()) {
            if (action.location.equals(location)) {
                return action;
            }
        }
        LOGGER.warn("Unknown action '{}'", location);
        return EMPTY;
    }

    record Action(ResourceLocation location, String description, Consumer<String> action) {
        void run(String context) {
            action.accept(context);
        }
    }
}
