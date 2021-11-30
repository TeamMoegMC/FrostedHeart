package com.teammoeg.frostedheart.research;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.LazyOptional;

public class FHResearch {
	public static ResearchRegistry researches=new ResearchRegistry();
	public static ClueRegistry clues=new ClueRegistry();
	private static Map<String,LazyOptional<Research>> researchCache=new HashMap<>();
	private static Map<String,LazyOptional<AbstractClue>> clueCache=new HashMap<>();
	public static CompoundNBT save(CompoundNBT cnbt) {
		cnbt.put("clues", clues.serialize());
		cnbt.put("researches", researches.serialize());
		return cnbt;
	}
	public void prepareReload() {
		researches.prepareReload();
		clues.prepareReload();
		researchCache.clear();
		clueCache.clear();
	}
	public static void load(CompoundNBT cnbt) {
		clues.deserialize(cnbt.getList("clues",0));
		researches.deserialize(cnbt.getList("researches",0));
	}
	private static final Function<String,LazyOptional<Research>> remapR=(n)->LazyOptional.<Research>of(()->researches.getByName(n));
	public static LazyOptional<Research> getLazyResearch(String id) {
		return researchCache.computeIfAbsent(id,remapR);
	}
	public static Supplier<Research> getResearch(String id) {
		return ()->getLazyResearch(id).orElse(null);
	}
	private static final Function<String,LazyOptional<AbstractClue>> remapC=(n)->LazyOptional.of(()->clues.getByName(n));
	public static LazyOptional<AbstractClue> getLazyClue(String id) {
		return clueCache.computeIfAbsent(id,remapC);
	}
	public static Supplier<AbstractClue> getClue(String id) {
		return ()->getLazyClue(id).orElse(null);
	}
}
