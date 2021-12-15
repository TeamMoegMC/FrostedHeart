package com.teammoeg.frostedheart.research;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ByteNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.LazyOptional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import dev.ftb.mods.ftbteams.data.Team;

public class TeamResearchData {
	public static TeamResearchData INSTANCE=new TeamResearchData(null);
	ArrayList<Boolean> clueComplete=new ArrayList<>();
	ArrayList<ResearchData> rdata=new ArrayList<>();
	int activeResearchId=0;
	CompoundNBT variants=new CompoundNBT();
	Supplier<Team> team;
	public TeamResearchData(Supplier<Team> team) {
		this.team = team;
	}
	public Optional<Team> getTeam() {
		if(team==null)return Optional.empty();
		return Optional.ofNullable(team.get());
	}
	public void triggerClue(int id) {
		setClueTriggered(id,true);
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
		getActiveResearch().ifPresent(r->this.getData(r).checkComplete());
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
			rnd=new ResearchData(FHResearch.getResearch(id),this);
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
	public LazyOptional<Research> getActiveResearch() {
		if(activeResearchId==0)
			return LazyOptional.empty();
		return LazyOptional.of(()->FHResearch.getResearch(activeResearchId).get());
	}
	public boolean canResearch() {
		LazyOptional<Research> rs=getActiveResearch();
		if(rs.isPresent()) {
			Research r=rs.resolve().get();
			return this.getData(r).canResearch();
		}
		return false;
	}
	
	//return excess items.
	public List<ItemStack> commitItem(ItemStack item){
		LazyOptional<Research> rs=getActiveResearch();
		if(rs.isPresent()) {
			Research r=rs.resolve().get();
			return this.getData(r).commitItem(item);
		}
		return Arrays.asList(new ItemStack[0]);
	}
	public void doResearch(int points){
		LazyOptional<Research> rs=getActiveResearch();
		if(rs.isPresent()) {
			Research r=rs.resolve().get();
			this.getData(r).doResearch(points);
		}
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
		rdata.stream().map(e->e!=null?e.serialize():new CompoundNBT()).forEach(e->rs.add(e));
		nbt.put("researches",rs);
		nbt.putInt("active",activeResearchId);
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
		ListNBT li=data.getList("researches",10);
		activeResearchId=data.getInt("active");
		for(int i=0;i<li.size();i++) {
			INBT e=li.get(i);
			if(e.getId()==10) {
				rdata.add(new ResearchData(FHResearch.getResearch(i+1),(CompoundNBT) e,this));
			}else
				rdata.add(null);
		}
	}
}
