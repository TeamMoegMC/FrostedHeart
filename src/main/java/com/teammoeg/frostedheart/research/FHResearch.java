package com.teammoeg.frostedheart.research;

import net.minecraft.nbt.CompoundNBT;

public class FHResearch {
	public static ResearchRegistry researches=new ResearchRegistry();
	public static ClueRegistry clues=new ClueRegistry();
	public static CompoundNBT save(CompoundNBT cnbt) {
		cnbt.put("clues", clues.serialize());
		cnbt.put("researches", researches.serialize());
		return cnbt;
	}
	public static void load(CompoundNBT cnbt) {
		clues.deserialize(cnbt.getList("clues",0));
		researches.deserialize(cnbt.getList("researches",0));
	}
}
