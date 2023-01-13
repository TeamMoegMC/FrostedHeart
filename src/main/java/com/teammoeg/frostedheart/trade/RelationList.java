package com.teammoeg.frostedheart.trade;

import java.util.Arrays;

import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntArrayNBT;

public class RelationList {
	public final int[] relations=new int[RelationModifier.values().length];
	Integer sum;
	public int get(RelationModifier relation) {
		return relations[relation.ordinal()];
	}
	public void put(RelationModifier relation,int val) {
		relations[relation.ordinal()]=val;
		sum=null;
	}
	public int sum() {
		if(sum==null) {
			sum=0;
			for(int i=0;i<relations.length;i++) {
				sum+=relations[i];
			}
		}
		return sum;
	}
	public INBT serialize() {
		return new IntArrayNBT(relations);
	}
	public void deserialize(INBT nbt) {
		
		if(nbt instanceof IntArrayNBT) {
			int[] is=((IntArrayNBT) nbt).getIntArray();
			Arrays.setAll(relations,i->is[i]);
			sum=null;
		}
	}
}
