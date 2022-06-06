package com.teammoeg.frostedheart.research;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.network.research.FHChangeActiveResearchPacket;
import com.teammoeg.frostedheart.research.ResearchListeners.BlockUnlockList;
import com.teammoeg.frostedheart.research.ResearchListeners.MultiblockUnlockList;
import com.teammoeg.frostedheart.research.ResearchListeners.RecipeUnlockList;
import com.teammoeg.frostedheart.research.clues.Clue;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.util.LazyOptional;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.PacketDistributor;

public class TeamResearchData {
	private static TeamResearchData INSTANCE=new TeamResearchData(null);
	ArrayList<Boolean> clueComplete=new ArrayList<>();
	ArrayList<Boolean> grantedEffects=new ArrayList<>();
	ArrayList<ResearchData> rdata=new ArrayList<>();
	int activeResearchId=0;
	CompoundNBT variants=new CompoundNBT();
	Supplier<Team> team;
	public RecipeUnlockList crafting=new RecipeUnlockList();
	public MultiblockUnlockList building=new MultiblockUnlockList();
	public BlockUnlockList block=new BlockUnlockList();
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
	public void triggerClue(Clue clue) {
		triggerClue(clue.getRId());
	}
	public void triggerClue(String lid) {
		triggerClue(FHResearch.clues.getByName(lid));
	}
	public void setClueTriggered(int id,boolean trig) {
		ensureClue(id);
		clueComplete.set(id-1,trig);
		getCurrentResearch().ifPresent(r->this.getData(r).checkComplete());
	}
	public void setClueTriggered(Clue clue,boolean trig) {
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
		if(clueComplete.size()>=id) {
			Boolean b=clueComplete.get(id-1);
			if(b!=null&&b==true)
				return true;
		}
		return false;
	}
	public boolean isClueTriggered(Clue clue){
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
	public LazyOptional<Research> getCurrentResearch() {
		if(activeResearchId==0)
			return LazyOptional.empty();
		return LazyOptional.of(()->FHResearch.getResearch(activeResearchId).get());
	}
	public void setCurrentResearch(Research r) {
		if(this.getData(r).active) {
			if(this.activeResearchId!=r.getRId()) {
				this.activeResearchId=r.getRId();
				FHChangeActiveResearchPacket packet = new FHChangeActiveResearchPacket(r);
				for (ServerPlayerEntity spe : team.get().getOnlineMembers())
					PacketHandler.send(PacketDistributor.PLAYER.with(() -> spe), packet);
			}
		}
	}
	public void clearCurrentResearch() {
		activeResearchId=0;
		FHChangeActiveResearchPacket packet = new FHChangeActiveResearchPacket();
		for (ServerPlayerEntity spe : team.get().getOnlineMembers())
			PacketHandler.send(PacketDistributor.PLAYER.with(() -> spe), packet);
	}
	public void clearCurrentResearch(Research r) {
		if(activeResearchId==r.getRId())
			clearCurrentResearch();
	}
	public boolean canResearch() {
		LazyOptional<Research> rs=getCurrentResearch();
		if(rs.isPresent()) {
			Research r=rs.resolve().get();
			return this.getData(r).canResearch();
		}
		return false;
	}
	public void ensureEffect(int len) {
		grantedEffects.ensureCapacity(len);
		while(grantedEffects.size()<len)
			grantedEffects.add(false);
	}
	public void grantEffect(int id) {
		ensureEffect(id-1);
		if(!grantedEffects.get(id-1)) {
			grantedEffects.set(id-1,FHResearch.effects.getById(id).grant(this, null));
		}
	}
	public void grantEffect(Effect e) {
		int id=e.getRId()-1;
		ensureEffect(id);
		if(!grantedEffects.get(id)) {
			grantedEffects.set(id,e.grant(this, null));
		}
	}
	public boolean isEffectGranted(int id) {
		if(grantedEffects.size()>=id) {
			return grantedEffects.get(id-1);
		}
		return false;
	}
	public boolean isEffectGranted(Effect e) {
		return isEffectGranted(e.getRId());
	}
	public void grantEffect(Effect e,ServerPlayerEntity player) {
		int id=e.getRId()-1;
		ensureEffect(id-1);
		if(!grantedEffects.get(id-1)) {
			grantedEffects.set(id-1,e.grant(this,player));
		}
	}
	public long doResearch(long points){
		LazyOptional<Research> rs=getCurrentResearch();
		if(rs.isPresent()) {
			Research r=rs.resolve().get();
			return this.getData(r).commitPoints(points);
		}
		return points;
	}
	public CompoundNBT serialize(boolean updatePacket) {
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
		//these data does not send to client
		if(!updatePacket) {
			nbt.put("crafting",crafting.serialize());
			nbt.put("building",building.serialize());
			nbt.put("block",block.serialize());
		}
		return nbt;
	}
	public CompoundNBT getVariants() {
		return variants;
	}
	public void deserialize(CompoundNBT data,boolean updatePacket) {
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
			rdata.add(new ResearchData(FHResearch.getResearch(i+1),(CompoundNBT) e,this));
		}
		if(!updatePacket) {
			crafting.load(data.getList("crafting",8));
			building.load(data.getList("building",8));
			block.load(data.getList("block",8));
		}

	}
	public static TeamResearchData getClientInstance() {
		return INSTANCE;
	}
	@OnlyIn(Dist.CLIENT)
	public static void setActiveResearch(int id) {
		INSTANCE.activeResearchId=id;
		
	}
}
