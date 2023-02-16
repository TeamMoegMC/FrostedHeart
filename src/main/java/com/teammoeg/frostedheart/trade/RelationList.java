package com.teammoeg.frostedheart.trade;

import net.minecraft.network.PacketBuffer;

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
	public void write(PacketBuffer pb) {
		pb.writeVarIntArray(relations);
	}
	public void read(PacketBuffer pb) {
		int[] arr=pb.readVarIntArray();
		int minl=Math.min(arr.length, relations.length);
		for(int i=0;i<minl;i++) {
			relations[i]=arr[i];
		}
	}
}
