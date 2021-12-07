package com.teammoeg.frostedheart.research;

import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

import java.util.ArrayList;

public class TeamResearchData {
	public static TeamResearchData INSTANCE=new TeamResearchData();
	ArrayList<Boolean> clueComplete=new ArrayList<>();
	ArrayList<ResearchData> rdata=new ArrayList<>();
	CompoundNBT variants=new CompoundNBT();
	public void triggerClue(int id) {
		ensureClue(id);
		clueComplete.set(id-1,true);
	}
	public void triggerClue(AbstractClue clue) {
		triggerClue(clue.getRId());
	}
	public void triggerClue(String lid) {
		triggerClue(FHResearch.clues.getByName(lid));
	}
	public void setClueTriggered(int id,boolean trig) {
		ensureClue(id);
		clueComplete.set(id-1,trig);
	}
	public void setClueTriggered(AbstractClue clue,boolean trig) {
		setClueTriggered(clue.getRId(),trig);
	}
	public void setClueTriggered(String lid,boolean trig) {
		setClueTriggered(FHResearch.clues.getByName(lid),trig);
	}
	public void ensureClue(int len) {
		clueComplete.ensureCapacity(len);
		while(clueComplete.size()<len)
			clueComplete.add(false);
	}
	public boolean isClueTriggered(int id){
		if(clueComplete.size()<=id) {
			Boolean b=clueComplete.get(id-1);
			if(b!=null&&b==true)
				return true;
		}
		return false;
	}
	public boolean isClueTriggered(AbstractClue clue){
		return isClueTriggered(clue.getRId());
	}
	public boolean isClueTriggered(String lid){
		return isClueTriggered(FHResearch.clues.getByName(lid));
	}
	public void ensureResearch(int len) {
		rdata.ensureCapacity(len);
		while(rdata.size()<len)
			rdata.add(null);
	}
	public ResearchData getData(int id) {
		ensureResearch(id);
		ResearchData rnd=rdata.get(id-1);
		if(rnd==null) {
			rnd=new ResearchData(FHResearch.getResearch(id));
			rdata.set(id-1,rnd);
		}
		return rnd;
	}
	public ResearchData getData(Research rs) {
		return getData(rs.getRId());
	}
	public ResearchData getData(String lid) {
		return getData(FHResearch.researches.getByName(lid));
	}
	public CompoundNBT serialize() {
		CompoundNBT nbt=new CompoundNBT();
		byte[] cl=new byte[clueComplete.size()];
		int i=-1;
		for(Boolean b:clueComplete) {
			cl[++i]=(byte) (b==null?0:(b?1:0));
		}
		nbt.putByteArray("clues",cl);
		nbt.put("vars",variants);
		ListNBT rs=new ListNBT();
		rdata.stream().map(e->e!=null?e.serialize():ByteNBT.ZERO).forEach(e->rs.add(e));
		nbt.put("researches",rs);
		return nbt;
	}
	public CompoundNBT getVariants() {
		return variants;
	}
	public void deserialize(CompoundNBT data) {
		clueComplete.clear();
		rdata.clear();
		byte[] ba=data.getByteArray("clues");
		ensureClue(ba.length);
		variants=data.getCompound("vars");
		for(int i=0;i<ba.length;i++)
			clueComplete.set(i,ba[i]!=0);
		data.getList("researches",0).stream().map(e->e.getId()==10?new ResearchData((CompoundNBT) e):null).forEach(e->rdata.add(e));;
	}
}
