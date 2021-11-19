package com.teammoeg.frostedheart.research;

import java.util.ArrayList;

import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

public class TeamResearchData {
	ArrayList<Boolean> clueComplete=new ArrayList<>();
	ArrayList<ResearchData> rdata=new ArrayList<>();
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
			rnd=new ResearchData();
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
		ListNBT cl=new ListNBT();
		clueComplete.stream().map(ByteNBT::valueOf).forEach(e->cl.add(e));
		nbt.put("clues",cl);
		ListNBT rs=new ListNBT();
		rdata.stream().map(e->e!=null?e.serialize():ByteNBT.ZERO).forEach(e->rs.add(e));
		nbt.put("researches",rs);
		return nbt;
	}
	public void deserialize(CompoundNBT data) {
		clueComplete.clear();
		rdata.clear();
		data.getList("clues",0).stream().map(e->((ByteNBT)e).getByte()!=0).forEach(e->clueComplete.add(e));
		data.getList("researches",0).stream().map(e->e.getId()==10?new ResearchData((CompoundNBT) e):null).forEach(e->rdata.add(e));;
	}
}
