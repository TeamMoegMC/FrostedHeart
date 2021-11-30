package com.teammoeg.frostedheart.research;

import java.util.function.Supplier;

import net.minecraft.nbt.CompoundNBT;

public class FHResearch {
	public static ResearchRegistry researches=new ResearchRegistry();
	public static ClueRegistry clues=new ClueRegistry();
	
	public static CompoundNBT save(CompoundNBT cnbt) {
		cnbt.put("clues", clues.serialize());
		cnbt.put("researches", researches.serialize());
		return cnbt;
	}
	public void prepareReload() {
		researches.prepareReload();
		clues.prepareReload();
	}
	public static void load(CompoundNBT cnbt) {
		clues.deserialize(cnbt.getList("clues",0));
		researches.deserialize(cnbt.getList("researches",0));
	}
	public static Supplier<Research> getResearch(String id) {
		return researches.get(id);
	}
	public static Supplier<AbstractClue> getClue(String id) {
		return clues.get(id);
	}
}
