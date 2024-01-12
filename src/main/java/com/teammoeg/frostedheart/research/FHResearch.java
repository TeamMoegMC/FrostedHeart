/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.research;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.research.clues.Clue;
import com.teammoeg.frostedheart.research.data.ClientResearchData;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.research.events.ResearchLoadEvent;
import com.teammoeg.frostedheart.research.research.Research;
import com.teammoeg.frostedheart.research.research.ResearchCategory;
import com.teammoeg.frostedheart.util.FileUtil;
import com.teammoeg.frostedheart.util.LazyOptional;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Main Research System.
 */
public class FHResearch {
    public static FHRegistry<Research> researches = new FHRegistry<Research>();
    public static FHRegistry<Clue> clues = new FHRegistry<Clue>();
    public static FHRegistry<Effect> effects = new FHRegistry<Effect>();
    private static LazyOptional<List<Research>> allResearches = LazyOptional.of(() -> researches.all());
    public static boolean editor = false;

    public static CompoundNBT save(CompoundNBT cnbt) {
        cnbt.put("clues", clues.serialize());
        cnbt.put("researches", researches.serialize());
        cnbt.put("effects", effects.serialize());
        return cnbt;
    }

    //register for after init
    public static void register(Research t) {
        researches.register(t);
        clearCache();
    }

    //called before reload
    public static void prepareReload() {
        researches.prepareReload();
        clues.prepareReload();
        effects.prepareReload();
        clearCache();
    }

    //clear cache when modification applied
    public static void clearCache() {
        allResearches = LazyOptional.of(() -> researches.all());
    }

    //called after reload
    public static void reindex() {
        allResearches.orElse(Collections.emptyList()).forEach(Research::doReindex);
        allResearches.orElse(Collections.emptyList()).forEach(Research::doIndex);
    }

    //called after reload
    public static void finishReload() {
        reindex();
        effects.all().forEach(Effect::init);
        clues.all().forEach(Clue::init);
    }

    public static void load(CompoundNBT cnbt) {
        clues.deserialize(cnbt.getList("clues", 8));
        researches.deserialize(cnbt.getList("researches", 8));
        effects.deserialize(cnbt.getList("effects", 8));
    }

    public static Supplier<Research> getResearch(String id) {
        return researches.get(id);
    }

    public static Supplier<Clue> getClue(String id) {
        return clues.get(id);
    }

    public static Supplier<Research> getResearch(int id) {
        return researches.get(id);
    }

    public static Supplier<Clue> getClue(int id) {
        return clues.get(id);
    }

    public static List<Research> getAllResearch() {
        return allResearches.resolve().get();
    }

    public static List<Research> getResearchesForRender(ResearchCategory cate, boolean showLocked) {
        List<Research> all = getAllResearch();
        ArrayList<Research> locked = new ArrayList<>();
        ArrayList<Research> available = new ArrayList<>();
        ArrayList<Research> unlocked = new ArrayList<>();
        ArrayList<Research> showed = new ArrayList<>();
        for (Research r : all) {
            if (r.getCategory() != cate) continue;
            if (r.isHidden()) {
                locked.add(r);
                continue;
            }

            if (r.isCompleted()) unlocked.add(r);
            else if (r.isUnlocked()) available.add(r);
            else if (r.isShowable()) showed.add(r);
            else locked.add(r);
        }

        available.ensureCapacity(available.size() + unlocked.size() + showed.size());
        available.addAll(showed);

        unlocked.removeIf(e -> {
            if (e.hasUnclaimedReward()) {
                available.add(0, e);
                return true;
            }
            return false;
        });
        available.addAll(unlocked);

        if (showLocked) available.addAll(locked);
        return available;
    }

    public static Research getFirstResearchInCategory(ResearchCategory cate) {
        List<Research> all = getAllResearch();
        Research unl = null;
        for (Research r : all) {
            if (r.getCategory() != cate) continue;
            if (r.isHidden()) continue;
            if (r.isCompleted() && unl == null) unl = r;
            else if (r.isUnlocked()) return r;
        }
        return unl;
    }

    public static void saveAll() {
        File folder = FMLPaths.CONFIGDIR.get().toFile();
        File rf = new File(folder, "fhresearches");
        rf.mkdirs();
        Gson gs = new GsonBuilder().setPrettyPrinting().create();
        for (Research r : getAllResearch()) {
            File out = new File(rf, r.getId() + ".json");
            try {
                FileUtil.transfer(gs.toJson(r.serialize()), out);
            } catch (IOException e) {
            }

        }
    }

    public static Research load(Research r) {
        File folder = FMLPaths.CONFIGDIR.get().toFile();
        File rf = new File(folder, "fhresearches");
        rf.mkdirs();
        File f = new File(rf, r.getId() + ".json");
        int iid = r.getRId();
        try {
            JsonElement je = new JsonParser().parse(FileUtil.readString(f));
            if (je.isJsonObject()) {
                r.load(je.getAsJsonObject());
            }
        } catch (IOException e) {
        }
        return researches.getById(iid);
    }

    public static void save(Research r) {
        File folder = FMLPaths.CONFIGDIR.get().toFile();
        File rf = new File(folder, "fhresearches");
        rf.mkdirs();
        Gson gs = new GsonBuilder().setPrettyPrinting().create();
        File out = new File(rf, r.getId() + ".json");
        try {
            FileUtil.transfer(gs.toJson(r.serialize()), out);
        } catch (IOException e) {
        }
    }

    public static void delete(Research r) {
        researches.remove(r);
        clearCache();
        File folder = FMLPaths.CONFIGDIR.get().toFile();
        File rf = new File(folder, "fhresearches");
        rf.mkdirs();
        new File(rf, r.getId() + ".json").delete();

    }

    public static void loadAll() {
        File folder = FMLPaths.CONFIGDIR.get().toFile();
        File rf = new File(folder, "fhresearches");
        rf.mkdirs();
        JsonParser jp = new JsonParser();

        for (File f : rf.listFiles((dir, name) -> name.endsWith(".json"))) {
            try {
                JsonElement je = jp.parse(FileUtil.readString(f));
                if (je.isJsonObject()) {
                    String id = f.getName();
                    researches.register(SpecialResearch.deserialize(id.substring(0, id.length() - 5), je.getAsJsonObject()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                FHMain.LOGGER.warn("Cannot load research " + f.getName() + ": " + e.getMessage());
            }

        }
    }

    public static void readAll(PacketBuffer pb) {
        List<Research> rss = SerializeUtil.readList(pb, SpecialResearch::deserialize);

        for (Research r : rss) {
            researches.register(r);
        }
    }

    public static void readAll(List<Research> rss) {

        for (Research r : rss) {
            r.packetInit();
            researches.register(r);
        }
    }

    public static void saveAll(PacketBuffer pb) {
        SerializeUtil.writeList(pb, getAllResearch(), Research::write);
    }

    public static boolean isEditor() {
        return editor;
    }

    public static void clearAll() {
        clues.clear();
        researches.clear();
        effects.clear();
    }

    public static void init() {
        ClientResearchData.last = null;
        ResearchListeners.reload();
        //No need to clear all as data manager would handle this.
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> TeamResearchData::resetClientInstance);
        prepareReload();
        MinecraftForge.EVENT_BUS.post(new ResearchLoadEvent.Pre());
        loadAll();
        MinecraftForge.EVENT_BUS.post(new ResearchLoadEvent.Post());
        finishReload();
        MinecraftForge.EVENT_BUS.post(new ResearchLoadEvent.Finish());
        //FHResearch.saveAll();
    }

    public static void initFromPacket(CompoundNBT data, List<Research> rs) {
        ClientResearchData.last = null;
        ResearchListeners.reload();
        //no need
        FHResearch.clearAll();
        prepareReload();
        MinecraftForge.EVENT_BUS.post(new ResearchLoadEvent.Pre());
        FHResearch.load(data);
        readAll(rs);
        MinecraftForge.EVENT_BUS.post(new ResearchLoadEvent.Post());
        finishReload();
        MinecraftForge.EVENT_BUS.post(new ResearchLoadEvent.Finish());
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> JEICompat::addInfo);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ResearchListeners::reloadEditor);
        //FHResearch.saveAll();
    }
}
