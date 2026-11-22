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

package com.teammoeg.frostedheart.content.stratadrill.client;

import com.teammoeg.chorda.client.model.BakedMaterialVariants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import se.mickelus.tetra.data.DataManager;
import se.mickelus.tetra.module.data.ImprovementData;
import se.mickelus.tetra.module.data.MaterialData;
import se.mickelus.tetra.module.data.ModuleData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Adapts normal synced Tetra cutter colors to S04 baked RGB. Not a gameplay/cache ID. */
final class StrataDrillCutterPalette {
    private static final String HEAD_SLOT = "stratadrill/head";
    private static final String CUTTER_KEY = "stratadrill/cutting_edge/";
    private static final String CUTTER_TAG = HEAD_SLOT + ":" + CUTTER_KEY;
    private static final ResourceLocation HEAD = new ResourceLocation("tetra", HEAD_SLOT);
    private static final ResourceLocation IMPROVEMENTS = new ResourceLocation("tetra", "stratadrill/cutting_edge");
    private static final ResourceLocation MATERIALS = new ResourceLocation("tetra", "stratadrill/cutters/");
    private static final StrataDrillCutterPalette EMPTY =
            new StrataDrillCutterPalette(Map.of(), BakedMaterialVariants.WHITE);

    private static volatile StrataDrillCutterPalette current = EMPTY;

    private final Map<String, Entry> byTag;
    private final int defaultRgb;

    private StrataDrillCutterPalette(Map<String, Entry> byTag, int defaultRgb) {
        this.byTag = byTag;
        this.defaultRgb = defaultRgb;
    }

    /** Registered only on the physical client with Tetra present. */
    static void bindTetra() {
        // Tetra applies its materials and expanded improvements before its modules. Unlike
        // model-bake events, this callback also runs when the server's data arrives after login.
        DataManager.instance.moduleData.onReload(StrataDrillCutterPalette::reload);
        MinecraftForge.EVENT_BUS.addListener(StrataDrillCutterPalette::disconnect);
    }

    static StrataDrillCutterPalette current() {
        return current;
    }

    private static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        current = EMPTY;
    }

    private static void reload() {
        DataManager data = DataManager.instance;
        ModuleData head = data.moduleData.getData(HEAD);
        ImprovementData[] improvements = data.improvementData.getData(IMPROVEMENTS);
        if (head == null || improvements == null || !Arrays.asList(head.improvements).contains(IMPROVEMENTS)) {
            current = EMPTY;
            return;
        }

        Map<String, Integer> byKey = new HashMap<>();
        for (MaterialData m : data.materialData.getDataIn(MATERIALS)) {
            if (m.key != null) {
                byKey.put(m.key, m.tints == null ? BakedMaterialVariants.WHITE
                        : m.tints.texture & BakedMaterialVariants.WHITE);
            }
        }
        Map<String, Entry> tags = new HashMap<>();
        int defaultColor = BakedMaterialVariants.WHITE;
        for (ImprovementData improvement : improvements) {
            if (!improvement.key.startsWith(CUTTER_KEY)) continue;
            String key = improvement.key.substring(CUTTER_KEY.length());
            Integer color = byKey.get(key);
            if (color == null) continue;
            tags.put(CUTTER_TAG + key, new Entry(improvement.level, color));
            if ("iron".equals(key)) defaultColor = color;
        }
        // Publish one immutable palette. Variant maps are owned and invalidated by the render thread.
        current = new StrataDrillCutterPalette(Map.copyOf(tags), defaultColor);
    }

    /** Resolve only once for a drill draw; no item ID, copied tag or per-face material query. */
    int rgb(ItemStack stack) {
        if (byTag.isEmpty()) return BakedMaterialVariants.WHITE;
        CompoundTag tag = stack.getTag();
        if (tag == null || !HEAD_SLOT.equals(tag.getString(HEAD_SLOT))) return BakedMaterialVariants.WHITE;
        boolean hasCutterTag = false;
        for (String key : tag.getAllKeys()) {
            if (!key.startsWith(CUTTER_TAG)) continue;
            hasCutterTag = true;
            Entry entry = byTag.get(key);
            if (entry != null && tag.get(key) instanceof NumericTag level && level.getAsInt() == entry.level) {
                return entry.rgb;
            }
        }
        // New and legacy stock heads contain an implicit iron blade. A dormant or unknown
        // explicit blade should be neutral, not look like some other loaded material.
        return hasCutterTag ? BakedMaterialVariants.WHITE : defaultRgb;
    }

    private record Entry(int level, int rgb) {}
}
