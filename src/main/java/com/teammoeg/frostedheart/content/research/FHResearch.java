/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.FHTeamDataManager;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.content.research.data.ClientResearchData;
import com.teammoeg.frostedheart.content.research.events.ResearchLoadEvent;
import com.teammoeg.frostedheart.content.research.network.FHResearchRegistrtySyncPacket;
import com.teammoeg.frostedheart.content.research.network.FHResearchSyncEndPacket;
import com.teammoeg.frostedheart.content.research.network.FHResearchSyncPacket;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.content.research.research.ResearchCategory;
import com.teammoeg.frostedheart.content.research.research.clues.Clue;
import com.teammoeg.frostedheart.content.research.research.effects.Effect;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import com.teammoeg.frostedheart.util.io.FileUtil;
import com.teammoeg.frostedheart.util.io.codec.DataOps;
import com.teammoeg.frostedheart.util.io.codec.ObjectWriter;
import com.teammoeg.frostedheart.util.utility.OptionalLazy;

import io.netty.buffer.Unpooled;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.PacketDistributor.PacketTarget;

/**
 * Main Research System.
 */
public class FHResearch {
	public static FHRegistry<Research> researches = new FHRegistry<>();
	public static FHRegistry<Clue> clues = new FHRegistry<>();
	public static FHRegistry<Effect> effects = new FHRegistry<>();
	private static OptionalLazy<List<Research>> allResearches = OptionalLazy.of(() -> researches.all());
	public static boolean editor = false;

	public static void clearAll() {
		clues.clear();
		researches.clear();
		effects.clear();
	}

	// clear cache when modification applied
	public static void clearCache() {
		allResearches = OptionalLazy.of(() -> researches.all());
	}

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
		effects.all().forEach(Effect::init);
		clues.all().forEach(Clue::init);
	}

	public static List<Research> getAllResearch() {
		return allResearches.resolve().get();
	}

	public static Supplier<Clue> getClue(int id) {
		return clues.get(id);
	}

	public static Supplier<Clue> getClue(String id) {
		return clues.get(id);
	}

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

	public static Supplier<Research> getResearch(int id) {
		return researches.get(id);
	}

	public static Supplier<Research> getResearch(String id) {
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

	public static void initFromRegistry(CompoundNBT data) {
		ClientResearchData.last = null;
		ResearchListeners.reload();

		// no need
		FHResearch.clearAll();
		prepareReload();
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

	public static void initFromPacket(CompoundNBT data, List<Research> rs) {
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

	public static void load(CompoundNBT cnbt) {
		clues.deserialize(cnbt.getList("clues", Constants.NBT.TAG_STRING));
		researches.deserialize(cnbt.getList("researches", Constants.NBT.TAG_STRING));
		effects.deserialize(cnbt.getList("effects", Constants.NBT.TAG_STRING));
	}

	public static Research load(Research r) {
		File folder = FMLPaths.CONFIGDIR.get().toFile();
		File rf = new File(folder, "fhresearches");
		rf.mkdirs();
		File f = new File(rf, r.getId() + ".json");
		int iid = FHResearch.researches.getIntId(r);
		try {
			JsonElement je = new JsonParser().parse(FileUtil.readString(f));
			if (je.isJsonObject()) {
				Research.CODEC.decode(JsonOps.INSTANCE, je).result().map(Pair::getFirst).map(o -> {
					o.setId(r.getId());
					return o;
				}).ifPresent(researches::replace);
				;
			}
		} catch (IOException e) {
			FHMain.LOGGER.error("Cannot load research " + f.getName() + ": " + e.getMessage());
		}
		return researches.getById(iid);
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
					id = id.substring(0, id.length() - 5);
					Research r = Research.CODEC.decode(JsonOps.INSTANCE, je).result().map(Pair::getFirst).orElse(null);
					if (r != null) {
						r.setId(id);
						researches.register(r);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				FHMain.LOGGER.warn("Cannot load research " + f.getName() + ": " + e.getMessage());
			}

		}
	}

	public static void main(String[] args) {
		System.out.println(Research.CODEC);
		File rf = new File("run/config/fhresearches");
		rf.mkdirs();
		JsonParser jp = new JsonParser();

		for (File f : rf.listFiles((dir, name) -> name.endsWith(".json"))) {
			try {
				JsonElement je = jp.parse(FileUtil.readString(f));
				if (je.isJsonObject()) {
					String id = f.getName();
					id = id.substring(0, id.length() - 5);
					Research r = Research.CODEC.decode(JsonOps.INSTANCE, je).result().map(Pair::getFirst).orElse(null);
					if (r != null) {
						r.setId(id);
						researches.register(r);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				FHMain.LOGGER.warn("Cannot load research " + f.getName() + ": " + e.getMessage());
			}

		}
		System.out.println(CodecUtil.INGREDIENT_CODEC.encodeStart(DataOps.COMPRESSED, Ingredient.fromItems(Items.ACACIA_BOAT)));
		PacketBuffer pb=new PacketBuffer(Unpooled.buffer());
		Object prein=Research.CODEC.encodeStart(DataOps.COMPRESSED, new Research()).resultOrPartial(System.out::println).get();
		System.out.println(prein);
		ObjectWriter.writeObject(pb,prein);
		System.out.println();
		System.out.println(pb.writerIndex());
		for(int i=0;i<pb.writerIndex();i++) {
			System.out.print(String.format("%2x ", pb.getByte(i)));
		}
		pb.resetReaderIndex();
		Object in=ObjectWriter.readObject(pb);
		System.out.println();
		System.out.println(in);
		System.out.println(Research.CODEC.decode(DataOps.COMPRESSED,in));
	}

	// called before reload
	public static void prepareReload() {
		researches.prepareReload();
		clues.prepareReload();
		effects.prepareReload();
		clearCache();
	}

	public static void readOne(Research r) {
		r.packetInit();
		researches.register(r);
	}

	public static void readAll(List<Research> rss) {

		for (Research r : rss) {
			readOne(r);
		}
	}

	// register for after init
	public static void register(Research t) {
		researches.register(t);
		clearCache();
	}

	// called after reload
	public static void reindex() {
		allResearches.orElse(Collections.emptyList()).forEach(Research::doReindex);
		allResearches.orElse(Collections.emptyList()).forEach(Research::doIndex);
	}

	public static CompoundNBT save(CompoundNBT cnbt) {
		cnbt.put("clues", clues.serialize());
		cnbt.put("researches", researches.serialize());
		cnbt.put("effects", effects.serialize());
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
		FHNetwork.send(target, new FHResearchRegistrtySyncPacket());
		FHResearch.getAllResearch().forEach(t -> FHNetwork.send(target, new FHResearchSyncPacket(t)));
		FHNetwork.send(target, new FHResearchSyncEndPacket());
	}

	private static final FolderName dataFolder = new FolderName("fhdata");

	public static void load() {
		FHResearch.editor = false;
		Path local = FHTeamDataManager.getServer().func_240776_a_(dataFolder);
		File regfile = new File(local.toFile().getParentFile(), "fhregistries.dat");
		FHResearch.clearAll();
		if (regfile.exists()) {
			try {
				FHResearch.load(CompressedStreamTools.readCompressed(regfile));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				FHMain.LOGGER.fatal("CANNOT READ RESEARCH REGISTRIES, MAY CAUSE UNSYNC!");

			}
		} else
			FHMain.LOGGER.error("NO REGISTRY FOUND");
		FHResearch.init();
		local.toFile().mkdirs();
		try {
			File dbg = new File(local.toFile().getParentFile(), "fheditor.dat");
			if (dbg.exists() && FileUtil.readString(dbg).equals("true"))
				FHResearch.editor = true;
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}

	public static void save() {
		Path local = FHTeamDataManager.getServer().func_240776_a_(dataFolder);
		File regfile = new File(local.toFile().getParentFile(), "fhregistries.dat");
		File dbg = new File(local.toFile().getParentFile(), "fheditor.dat");
		try {
			if (FHResearch.isEditor())
				FileUtil.transfer("true", dbg);
			else if (dbg.exists())
				FileUtil.transfer("false", dbg);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		try {
			CompressedStreamTools.writeCompressed(FHResearch.save(new CompoundNBT()), regfile);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			FHMain.LOGGER.fatal("CANNOT SAVE RESEARCH REGISTRIES, MAY CAUSE UNSYNC!");
		}
	}
}
