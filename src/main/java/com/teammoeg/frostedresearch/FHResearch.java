/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedresearch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.FileUtil;
import com.teammoeg.chorda.io.codec.DataOps;
import com.teammoeg.chorda.io.codec.ObjectWriter;
import com.teammoeg.chorda.util.CDistHelper;
import com.teammoeg.frostedresearch.compat.JEICompat;
import com.teammoeg.frostedresearch.data.ClientResearchData;
import com.teammoeg.frostedresearch.events.ResearchLoadEvent;
import com.teammoeg.frostedresearch.network.FHResearchRegistrtySyncPacket;
import com.teammoeg.frostedresearch.network.FHResearchSyncEndPacket;
import com.teammoeg.frostedresearch.network.FHResearchSyncPacket;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.ResearchCategory;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor.PacketTarget;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Main Research System.
 */
public class FHResearch {
    /**
     * Registry holder for all defined Research in config.
     */
    public static FHRegistry<Research> researches = new FHRegistry<>();
    public static final LevelResource dataFolder = new LevelResource("fhresearch_data");
    /**
     * Editing mode.
     */
    public static boolean editor = false;
    /**
     * Cache for all Researches.
     */
    private static Lazy<List<Research>> allResearches = Lazy.of(() -> researches.all());

    public static void clearAll() {
        //clues.clear();
        researches.clear();
        //effects.clear();
    }

    // clear cache when modification applied
    public static void clearCache() {
            allResearches = Lazy.of(() -> researches.all());
    }

    /**
     * Delete a Research from config JSON
     *
     * @param r Research to delete
     */
    public static void delete(Research r) {
        researches.remove(r);
        clearCache();
        File folder = FMLPaths.CONFIGDIR.get().toFile();
        File rf = new File(folder, "fhresearches");
        rf.mkdirs();
        new File(rf, r.getId() + ".json").delete();

    }

    // called after reload
    public static void finishReload() {
        reindex();
		/*effects.all().forEach(Effect::init);
		clues.all().forEach(Clue::init);*/
    }

    public static List<Research> getAllResearch() {
        return allResearches.get();
    }

	/*public static Clue getClue(int id) {
		return clues.get(id);
	}

	public static Clue getClue(String id) {
		return clues.get(id);
	}*/

    public static Research getFirstResearchInCategory(ResearchCategory cate) {
        List<Research> all = getAllResearch();
        Research unl = null;
        for (Research r : all) {
            if (r.getCategory() != cate)
                continue;
            if (r.isHidden())
                continue;
            if (r.isCompleted() && unl == null)
                unl = r;
            else if (r.isUnlocked())
                return r;
        }
        return unl;
    }

    public static Research getResearch(int id) {
        return researches.get(id);
    }

    public static Research getResearch(String id) {
        return researches.get(id);
    }

    public static List<Research> getResearchesForRender(ResearchCategory cate, boolean showLocked) {
        List<Research> all = getAllResearch();
        ArrayList<Research> locked = new ArrayList<>();
        ArrayList<Research> available = new ArrayList<>();
        ArrayList<Research> unlocked = new ArrayList<>();
        ArrayList<Research> showed = new ArrayList<>();
        for (Research r : all) {
            if (r.getCategory() != cate)
                continue;
            if (r.isHidden()) {
                locked.add(r);
                continue;
            }
            if (r.isCompleted())
                unlocked.add(r);
            else if (r.isUnlocked())
                available.add(r);
            else if (r.isShowable())
                showed.add(r);
            else
                locked.add(r);
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

        if (showLocked)
            available.addAll(locked);
        return available;
    }

    /**
     * Initialization steps after Research data is loaded.
     */
    public static void init() {
        ClientResearchData.last = null;
        ResearchListeners.reload();
        // No need to clear all as data manager would handle this.

        // FHResearch.clearAll();
        prepareReload();
        MinecraftForge.EVENT_BUS.post(new ResearchLoadEvent.Pre());
        loadAll();
        MinecraftForge.EVENT_BUS.post(new ResearchLoadEvent.Post());
        finishReload();
        MinecraftForge.EVENT_BUS.post(new ResearchLoadEvent.Finish());
        // FHResearch.saveAll();
    }

    public static void initFromRegistry(CompoundTag data) {
        ClientResearchData.last = null;
        ResearchListeners.reload();

        // no need
        FHResearch.clearAll();
        prepareReload();
        //clearCache();
        MinecraftForge.EVENT_BUS.post(new ResearchLoadEvent.Pre());
        FHResearch.load(data);
    }

    public static void endPacketInit() {
        MinecraftForge.EVENT_BUS.post(new ResearchLoadEvent.Post());
        finishReload();
        MinecraftForge.EVENT_BUS.post(new ResearchLoadEvent.Finish());
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> JEICompat::addInfo);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ResearchListeners::reloadEditor);
    }

    public static void initFromPacket(CompoundTag data, List<Research> rs) {
        ClientResearchData.last = null;
        ResearchListeners.reload();
        // no need
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
        // FHResearch.saveAll();
    }

    public static boolean isEditor() {
        return editor;
    }

    public static void load(CompoundTag cnbt) {
        //clues.deserialize(cnbt.getList("clues", Tag.TAG_STRING));
        researches.deserialize(cnbt.getList("researches", Tag.TAG_STRING));
        //effects.deserialize(cnbt.getList("effects", Tag.TAG_STRING));
    }

    /**
     * Load Research from config JSON
     *
     * @param r Research to load
     * @return Loaded Research
     */
    public static Research load(Research r) {
        File folder = FMLPaths.CONFIGDIR.get().toFile();
        File rf = new File(folder, "fhresearches");
        rf.mkdirs();
        File f = new File(rf, r.getId() + ".json");
        int iid = FHResearch.researches.getIntId(r);
        try {
            JsonElement je = new JsonParser().parse(FileUtil.readString(f));
            if (je.isJsonObject()) {

                Research.CODEC.parse(JsonOps.INSTANCE, je).resultOrPartial(FRMain.LOGGER::error).map(o -> {
                    o.setId(r.getId());
                    //System.out.println(o);
                    return o;
                }).ifPresent(researches::replace);
                ;

            }

        } catch (IOException e) {
            FRMain.LOGGER.error("Cannot load research " + f.getName() + ": " + e.getMessage());
        }
        clearCache();
        return researches.get(iid);
    }

    /**
     * Load all Research from config JSON
     */
    public static void loadAll() {
        File folder = FMLPaths.CONFIGDIR.get().toFile();
        File rf = new File(folder, "fhresearches");
        rf.mkdirs();
        JsonParser jp = new JsonParser();
        FRMain.LOGGER.info("loading research from files...");
        for (File f : rf.listFiles((dir, name) -> name.endsWith(".json"))) {
            try {
                JsonElement je = jp.parse(FileUtil.readString(f));
                if (je.isJsonObject()) {
                    String id = f.getName();
                    id = id.substring(0, id.length() - 5);
                    Research r = Research.CODEC.parse(JsonOps.INSTANCE, je).resultOrPartial(FRMain.LOGGER::error).orElse(null);
                    if (r != null) {
                        r.setId(id);
                        researches.register(r);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                FRMain.LOGGER.warn("Cannot load research " + f.getName() + ": " + e.getMessage());
            }

        }
    }

    public static void main(String[] args) {
//		System.out.println(Research.CODEC);
        File rf = new File("run/config/fhresearches");
        rf.mkdirs();
        JsonParser jp = new JsonParser();

        for (File f : rf.listFiles((dir, name) -> name.endsWith(".json"))) {
            try {
                JsonElement je = jp.parse(FileUtil.readString(f));
                if (je.isJsonObject()) {
                    String id = f.getName();
                    id = id.substring(0, id.length() - 5);
                    Research r = Research.CODEC.parse(JsonOps.INSTANCE, je).result().orElse(null);
                    if (r != null) {
                        r.setId(id);
                        researches.register(r);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                FRMain.LOGGER.warn("Cannot load research " + f.getName() + ": " + e.getMessage());
            }

        }
//		System.out.println(CodecUtil.INGREDIENT_CODEC.encodeStart(DataOps.COMPRESSED, Ingredient.of(Items.ACACIA_BOAT)));
        FriendlyByteBuf pb = new FriendlyByteBuf(Unpooled.buffer());
        Object prein = Research.CODEC.encodeStart(DataOps.COMPRESSED, new Research()).resultOrPartial(FRMain.LOGGER::debug).get();
//		System.out.println(prein);
        ObjectWriter.writeObject(pb, prein);
//		System.out.println();
//		System.out.println(pb.writerIndex());
//		for(int i=0;i<pb.writerIndex();i++) {
//			System.out.print(String.format("%2x ", pb.getByte(i)));
//		}
        pb.resetReaderIndex();
        Object in = ObjectWriter.readObject(pb);
//		System.out.println();
//		System.out.println(in);
//		System.out.println(Research.CODEC.parse(DataOps.COMPRESSED,in));
    }

    // called before reload
    public static void prepareReload() {
        researches.prepareReload();
        //clues.prepareReload();
        //effects.prepareReload();
        clearCache();
    }

    public static void readOne(String key, Research r) {
        r.setId(key);
        r.packetInit();
        researches.register(r);
        clearCache();
    }

    public static void readAll(List<Research> rss) {

        for (Research r : rss) {
            r.packetInit();
            researches.register(r);
        }
    }

    // register for after init
    public static void register(Research t) {
        researches.register(t);
        clearCache();
    }

    // called after reload
    public static void reindex() {
        try {
            allResearches.get().forEach(Research::doReindex);
            allResearches.get().forEach(Research::doIndex);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static CompoundTag save(CompoundTag cnbt) {
        //cnbt.put("clues", clues.serialize());
        cnbt.put("researches", researches.serialize());
        //cnbt.put("effects", effects.serialize());
        return cnbt;
    }

    public static void save(Research r) {
        File folder = FMLPaths.CONFIGDIR.get().toFile();
        File rf = new File(folder, "fhresearches");
        rf.mkdirs();
        Gson gs = new GsonBuilder().setPrettyPrinting().create();
        File out = new File(rf, r.getId() + ".json");
        try {
            FileUtil.transfer(gs.toJson(CodecUtil.encodeOrThrow(Research.CODEC.encodeStart(JsonOps.INSTANCE, r))),
                    out);
        } catch (IOException e) {

            throw new RuntimeException("Cannot save research " + r.getId() + ": " + e.getMessage());
        }
    }

    public static void saveAll() {
        File folder = FMLPaths.CONFIGDIR.get().toFile();
        File rf = new File(folder, "fhresearches");
        rf.mkdirs();
        Gson gs = new GsonBuilder().setPrettyPrinting().create();
        for (Research r : getAllResearch()) {
            File out = new File(rf, r.getId() + ".json");
            try {
                FileUtil.transfer(
                        gs.toJson(CodecUtil.encodeOrThrow(Research.CODEC.encodeStart(JsonOps.INSTANCE, r))), out);
            } catch (IOException e) {
                throw new RuntimeException("Cannot save research " + r.getId() + ": " + e.getMessage());
            }

        }
    }

    public static void sendSyncPacket(PacketTarget target) {
    	FRNetwork.INSTANCE.send(target, new FHResearchRegistrtySyncPacket());
        FHResearch.getAllResearch().forEach(t -> FRNetwork.INSTANCE.send(target, new FHResearchSyncPacket(t)));
        FRNetwork.INSTANCE.send(target, new FHResearchSyncEndPacket());
    }

    /**
     * Load Research data from disk.
     */
    public static void load() {
        FHResearch.editor = false;
        Path local = CDistHelper.getServer().getWorldPath(dataFolder);
        File regfile = new File(local.toFile().getParentFile(), "fhregistries.dat");
        FHResearch.clearAll();
        if (regfile.exists()) {
            try {
                FHResearch.load(NbtIo.readCompressed(regfile));
                FRMain.LOGGER.info("Research registries loaded.");
            } catch (IOException e) {
                e.printStackTrace();
                FRMain.LOGGER.fatal("CANNOT READ RESEARCH REGISTRIES, MAY CAUSE UNSYNC!");

            }
        } else {
            FRMain.LOGGER.error("No registry file found when loading research data.");
        }
        FHResearch.init();
        FRMain.LOGGER.info("RESEARCH DATA INITIALIZED");
        local.toFile().mkdirs();
        try {
            File dbg = new File(local.toFile().getParentFile(), "fheditor.dat");
            if (dbg.exists() && FileUtil.readString(dbg).equals("true"))
                FHResearch.editor = true;
        } catch (IOException e2) {
            FRMain.LOGGER.error("Cannot read editor status");
            e2.printStackTrace();
        }
    }

    /**
     * Save Research data to disk.
     */
    public static void save() {
        Path local = CDistHelper.getServer().getWorldPath(dataFolder);
        File regfile = new File(local.toFile().getParentFile(), "fhregistries.dat");
        File dbg = new File(local.toFile().getParentFile(), "fheditor.dat");
        try {
            if (FHResearch.isEditor())
                FileUtil.transfer("true", dbg);
            else if (dbg.exists())
                FileUtil.transfer("false", dbg);
        } catch (IOException e2) {
            FRMain.LOGGER.error("Cannot save editor status");
            e2.printStackTrace();
        }
        try {
            NbtIo.writeCompressed(FHResearch.save(new CompoundTag()), regfile);
            FRMain.LOGGER.info("Research Registries saved.");
        } catch (IOException e1) {
            e1.printStackTrace();
            FRMain.LOGGER.fatal("CANNOT SAVE RESEARCH REGISTRIES, MAY CAUSE UNSYNC!");
        }
    }
}
