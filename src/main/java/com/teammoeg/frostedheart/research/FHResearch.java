package com.teammoeg.frostedheart.research;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.research.clues.Clue;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.util.FileUtil;
import com.teammoeg.frostedheart.util.LazyOptional;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fml.loading.FMLPaths;
/**
 * Main Research System.
 * 
 * */
public class FHResearch {
	public static FHRegistry<Research> researches=new FHRegistry<Research>();
	public static FHRegistry<Clue> clues=new FHRegistry<Clue>();
	public static FHRegistry<Effect> effects=new FHRegistry<Effect>();
	private static LazyOptional<List<Research>> allResearches=LazyOptional.of(()->researches.all());
	private static LazyOptional<List<Clue>> allClues=LazyOptional.of(()->clues.all());
	public static boolean editor=false;
	public static CompoundNBT save(CompoundNBT cnbt) {
		cnbt.put("clues", clues.serialize());
		cnbt.put("researches", researches.serialize());
		return cnbt;
	}
	public static void register(Research t) {
		researches.register(t);
	}
	//called before reload
	public static void prepareReload() {
		researches.prepareReload();
		clues.prepareReload();
		effects.prepareReload();
		allResearches=LazyOptional.of(()->researches.all());
		allClues=LazyOptional.of(()->clues.all());
	}
	//called after reload
	public static void finishReload() {
		allResearches.orElse(Collections.emptyList()).forEach(Research::doIndex);
		effects.all().forEach(Effect::init);
		clues.all().forEach(Clue::init);
	}
	public static void load(CompoundNBT cnbt) {
		clues.deserialize(cnbt.getList("clues",8));
		researches.deserialize(cnbt.getList("researches",8));
		effects.deserialize(cnbt.getList("effects",8));
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
	public static List<Research> getResearchesForRender(ResearchCategory cate, boolean showLocked){
		List<Research> all= getAllResearch();
		ArrayList<Research> locked=new ArrayList<>();
		ArrayList<Research> available=new ArrayList<>();
		ArrayList<Research> unlocked=new ArrayList<>();
		for(Research r:all) {
			if(r.getCategory()!=cate)continue;
			if(r.isCompleted())unlocked.add(r);
			else if(r.isUnlocked())available.add(r);
			else locked.add(r);
		}
		available.ensureCapacity(available.size()+unlocked.size()+locked.size());
		available.addAll(unlocked);
		if (showLocked) available.addAll(locked);
		return available;
	}

	public static Research getFirstResearchInCategory(ResearchCategory cate) {
		List<Research> all= getAllResearch();
		Research unl=null;
		for(Research r:all) {
			if(r.getCategory()!=cate)continue;
			if(r.isCompleted()&&unl==null)unl=r;
			else if(r.isUnlocked())return r;
		}
		return unl;
	}

	public static void saveAll() {
		File folder=FMLPaths.CONFIGDIR.get().toFile();
		File rf=new File(folder,"fhresearches");
		rf.mkdirs();
		Gson gs=new GsonBuilder().setPrettyPrinting().create();
		for(Research r:getAllResearch()) {
			File out=new File(rf,r.getId()+".json");
			try {
				FileUtil.transfer(gs.toJson(r.serialize()),out);
			} catch (IOException e) {
			}
			
		}
	}
	public static List<Clue> getAllClue() {
		return allClues.resolve().get();
	}
	public static void loadAll() {
		File folder=FMLPaths.CONFIGDIR.get().toFile();
		File rf=new File(folder,"fhresearches");
		rf.mkdirs();
		JsonParser jp=new JsonParser();
		
		for(File f:rf.listFiles((dir,name)->name.endsWith(".json"))) {
			try(FileReader fr=new FileReader(f);JsonReader jr=new JsonReader(fr)){
				JsonElement je=jp.parse(jr);
				if(je.isJsonObject()) {
					String id=f.getName();
					register(new Research(id.substring(0,id.length()-5),je.getAsJsonObject()));
				}
			} catch (Exception e) {
				e.printStackTrace();
				FHMain.LOGGER.warn("Cannot load research "+f.getName()+": "+e.getMessage());
			}
			
		}
	}
	public static boolean isEditor() {
		return editor;
	}
}
