package com.teammoeg.frostedheart.research;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.util.LazyOptional;

import net.minecraft.nbt.CompoundNBT;

public class FHResearch {
	public static ResearchRegistry researches=new ResearchRegistry();
	public static ClueRegistry clues=new ClueRegistry();
	private static LazyOptional<List<Research>> allResearches=LazyOptional.of(()->researches.all());
	private static LazyOptional<List<AbstractClue>> allClues=LazyOptional.of(()->clues.all());
	public static CompoundNBT save(CompoundNBT cnbt) {
		cnbt.put("clues", clues.serialize());
		cnbt.put("researches", researches.serialize());
		return cnbt;
	}
	public static void register(Research t) {
		researches.register(t);
	}
	public static void register(AbstractClue t) {
		clues.register(t);
	}
	//called before reload
	public static void prepareReload() {
		researches.prepareReload();
		clues.prepareReload();
		allResearches=LazyOptional.of(()->researches.all());
		allClues=LazyOptional.of(()->clues.all());
	}
	//called after reload
	public static void indexResearches() {
		allResearches.orElse(Collections.emptyList()).forEach(c->c.doIndex());
	}
	public static void load(CompoundNBT cnbt) {
		clues.deserialize(cnbt.getList("clues",8));
		researches.deserialize(cnbt.getList("researches",8));
		
	}
	public static Supplier<Research> getResearch(String id) {
		return researches.get(id);
	}
	public static Supplier<AbstractClue> getClue(String id) {
		return clues.get(id);
	}
	public static Supplier<Research> getResearch(int id) {
		return researches.get(id);
	}
	public static Supplier<AbstractClue> getClue(int id) {
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
	public static List<AbstractClue> getAllClue() {
		return allClues.resolve().get();
	}
}
