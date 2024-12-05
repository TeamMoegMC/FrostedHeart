/*
 * Copyright (c) 2022-2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.research.data;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.team.SpecialData;
import com.teammoeg.frostedheart.base.team.SpecialDataHolder;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.research.FHRegistry;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.ResearchListeners.BlockUnlockList;
import com.teammoeg.frostedheart.content.research.ResearchListeners.CategoryUnlockList;
import com.teammoeg.frostedheart.content.research.ResearchListeners.MultiblockUnlockList;
import com.teammoeg.frostedheart.content.research.ResearchListeners.RecipeUnlockList;
import com.teammoeg.frostedheart.content.research.events.ResearchStatusEvent;
import com.teammoeg.frostedheart.content.research.network.FHChangeActiveResearchPacket;
import com.teammoeg.frostedheart.content.research.network.FHS2CClueProgressSyncPacket;
import com.teammoeg.frostedheart.content.research.network.FHEffectProgressSyncPacket;
import com.teammoeg.frostedheart.content.research.network.FHResearchAttributeSyncPacket;
import com.teammoeg.frostedheart.content.research.network.FHResearchDataSyncPacket;
import com.teammoeg.frostedheart.content.research.network.FHResearchDataUpdatePacket;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.content.research.research.clues.Clue;
import com.teammoeg.frostedheart.content.research.research.clues.ItemClue;
import com.teammoeg.frostedheart.content.research.research.effects.Effect;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import com.teammoeg.frostedheart.util.utility.OptionalLazy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;


/**
 * Class TeamResearchData.
 * <p>
 * This stores all data for research, restrictions, locks, clues and points data.
 * This data is available for both team and player.
 *
 * @author khjxiaogu
 * @date 2022/9/2
 */
public class TeamResearchData implements SpecialData{
    /**
     * The Insights point.<br>
     *
     * Insights are rewarded by active actions and new discoveries.
     *
     * Insights is monotone increasing.
     */
    int insight = 0;
    /**
     * The Insights level.<br>
     *
     * Computed from insights point.
     *
     * Insights level is a monotone increasing function of insights point.
     */
    int insightLevel = 0;
    /**
     * The used insights level.<br>
     *
     * Completing one research would increment this value.
     */
    int usedInsightLevel = 0;

    /**
     * The rdata.<br>
     */
    ArrayList<ResearchData> rdata = new ArrayList<>();

    /**
     * The active research id.<br>
     */
    int activeResearchId = 0;

    /**
     * The variants.<br>
     */
    CompoundTag variants = new CompoundTag();

    

    /**
     * The crafting.<br>
     */
    public RecipeUnlockList crafting = new RecipeUnlockList();

    /**
     * The building.<br>
     */
    public MultiblockUnlockList building = new MultiblockUnlockList();

    /**
     * The block.<br>
     */
    public BlockUnlockList block = new BlockUnlockList();

    /**
     * The categories.<br>
     */
    public CategoryUnlockList categories = new CategoryUnlockList();

    public TeamResearchData(SpecialDataHolder team) {
    }

    /**
     * Check can research now.<br>
     *
     * @return true, if a research is selected and it is ready for research
     */
    public boolean canResearch() {
        OptionalLazy<Research> rs = getCurrentResearch();
        if (rs.isPresent()) {
            Research r = rs.resolve().get();
            return this.getData(r).canResearch();
        }
        return false;
    }
	public void setHolder(SpecialDataHolder holder) {};
    /**
     * Clear current research.
     *
     * @param sync send update packet
     */
    public void clearCurrentResearch(TeamDataHolder team,boolean sync) {
        if (activeResearchId == 0) return;
        Research r = FHResearch.researches.getById(activeResearchId);
        activeResearchId = 0;
        if (r != null)
	        if (team!=null) {
    			for (Clue c : r.getClues())
    				c.end(team,r);
    			if(sync)
    				team.sendToOnline(new FHChangeActiveResearchPacket());
	        }
    }

    /**
     * Clear current research.
     *
     * @param r the r<br>
     */
    @OnlyIn(Dist.CLIENT)
    public void clearCurrentResearch(Research r) {
        if (activeResearchId == FHResearch.researches.getIntId(r))
        	activeResearchId=0;
    }


    /**
     * Commit research points to current research.<br>
     *
     * @param points the points<br>
     * @return unused points after commit to current research.
     */
    public long doResearch(TeamDataHolder team,long points) {
        OptionalLazy<Research> rs = getCurrentResearch();
        if (rs.isPresent()) {
            Research r = rs.resolve().get();
            ResearchData rd = this.getData(r);
            if (!rd.active || rd.finished)
                return points;
            return rd.commitPoints(r, points, ()->{
            	checkResearchComplete(team,r);
            	sendResearchProgressPacket(team, r);
            });
        }
        return points;
    }
    public boolean commitItem(ServerPlayer player,TeamDataHolder team,Research research) {
        if (research.isInCompletable()) return false;
        for (Research par : research.getParents()) {
            if (!getData(par).isCompleted()) {
                return false;
            }
        }
        if(!research.getRequiredItems().isEmpty()&&!FHUtils.costItems(player,research.getRequiredItems()))
        	return false;
        getData(research).setActive();
        this.sendResearchProgressPacket(team, research);
        return true;
    }
	public boolean isClueCompleted(Research par,Clue clue) {
		return getData(par).isClueTriggered(clue);
	}
    public void setClueCompleted(TeamDataHolder team,Research par,int clue, boolean trig) {
    	
    	Clue cl=par.getClues().get(clue);
    	if(cl!=null) {
    		if(isClueCompleted(par,cl)==trig)return;
	    	getData(par).setClueTriggered(cl.getNonce(), trig);
	        if (trig)
	            cl.end(team,par);
	        else
	        	cl.start(team,par);
	        sendClueProgressPacket(team,par,clue,trig);
    	}
    }
    public void setClueCompleted(TeamDataHolder team,Research par,Clue clue, boolean trig) {
    	if(isClueCompleted(par,clue)==trig)return;
    	getData(par).setClueTriggered(clue.getNonce(), trig);
        if (trig)
        	clue.end(team,par);
        else
        	clue.start(team,par);
        sendClueProgressPacket(team,par,par.getClues().indexOf(clue),trig);
    }

	public boolean isEffectGranted(Research research, Effect e) {
		return getData(research).isEffectGranted(e);
	}

	public void setEffectGranted(Research rch, Effect effect, boolean b) {
		
		getData(rch).setEffectGranted(effect, b);
		
	}

    public boolean checkResearchComplete(TeamDataHolder team, Research r) {
    	ResearchData rd=getData(r);
        if (rd.finished)
            return false;
        boolean flag = true;
        for (Clue cl : r.getClues()) {
            if (cl.isRequired() && !rd.isClueTriggered(cl)) {
                flag = false;
                break;
            }
        }
        if (rd.getTotalCommitted(r) >= r.getRequiredPoints() && flag) {
            rd.setFinished(true);
            this.grantEffects(team, null, r);
            this.annouceResearchComplete(team,r);
            return true;
        }
        return false;
    }
    public void setResearchFinished(TeamDataHolder team,Research rs,boolean data) {
    	ResearchData rd=getData(rs);
    	rd.setFinished(true);
        this.grantEffects(team, null, rs);
        this.annouceResearchComplete(team,rs);
    }
    /**
     * Grant effects.
     *
     * @param team the team<br>
     * @param spe  the spe<br>
     */
    public void grantEffects(TeamDataHolder holder, ServerPlayer spe,Research rs) {
        boolean granted = true;
        ResearchData rd=getData(rs);
        for (Effect e : rs.getEffects()) {
            grantEffect(holder,rs,e, spe);
            granted &= rd.isEffectGranted(e);
        }
        if (rs.isInfinite() && granted) {
            int lvl = rd.getLevel();
            this.resetData(holder, rs);
            rd.setLevel(lvl + 1);
        }

    }
    private void sendClueProgressPacket(TeamDataHolder team, Research par, int clue, boolean trig) {
		team.sendToOnline(new FHS2CClueProgressSyncPacket(trig,FHResearch.researches.getIntId(par),clue));
	}
    /**
     * Send effect progress packet for current effect to players in team.
     * Useful for data sync. This would called automatically, Their's no need to call this in effect.
     *
     * @param team the team<br>
     */
    private void sendEffectProgressPacket(TeamDataHolder team, Research par, int effect, boolean trig) {
        FHEffectProgressSyncPacket packet = new FHEffectProgressSyncPacket(trig,FHResearch.researches.getIntId(par),effect);
        team.sendToOnline(packet);
    }
    private void annouceResearchComplete(TeamDataHolder team, Research par) {
    	team.getTeam().ifPresent(e -> MinecraftForge.EVENT_BUS.post(new ResearchStatusEvent(par, e, getData(par).finished)));
    }
    private void sendResearchProgressPacket(TeamDataHolder team, Research par) {
        team.sendToOnline(new FHResearchDataUpdatePacket(par,getData(par)));
    }
	/**
     * Ensure clue data length.
     *
     * @param len the len<br>
     */
    private void ensureClue(int len) {
    }

    /**
     * Ensure effect data length.
     *
     * @param len the len<br>
     */
    public void ensureEffect(int len) {
    }

    /**
     * Ensure research data length.
     *
     * @param len the len<br>
     */
    public void ensureResearch(int len) {
        rdata.ensureCapacity(len);
        while (rdata.size() < len)
            rdata.add(null);
    }

    /**
     * Get current research.
     *
     * @return current research<br>
     */
    public OptionalLazy<Research> getCurrentResearch() {
        if (activeResearchId == 0)
            return OptionalLazy.empty();
        return OptionalLazy.of(() -> FHResearch.getResearch(activeResearchId));
    }

    /**
     * Get research data.
     *
     * @param id the id<br>
     * @return data<br>
     */
    public ResearchData getData(int id) {
        if (id <= 0) return null;
        ensureResearch(id);
        ResearchData rnd = rdata.get(id - 1);
        if (rnd == null) {
            rnd = new ResearchData();
            rdata.set(id - 1, rnd);
        }
        return rnd;
    }
    public ResearchData setData(int id,ResearchData rd) {
        if (id <= 0) return null;
        ensureResearch(id);
        ResearchData rnd = rdata.set(id - 1,rd);
        return rnd;
    }
    /**
     * Get research data.
     *
     * @param rs the rs<br>
     * @return data<br>
     */
    public ResearchData getData(Research rs) {
        if (rs == null) return ResearchData.EMPTY;
        return getData(FHResearch.researches.getIntId(rs));
    }

    /**
     * Get research data.
     *
     * @param lid the lid<br>
     * @return data<br>
     */
    public ResearchData getData(String lid) {
        return getData(FHResearch.researches.getByName(lid));
    }




    public double getVariantDouble(ResearchVariant name) {
        return variants.getDouble(name.getToken());

    }

    public long getVariantLong(ResearchVariant name) {
        return variants.getLong(name.getToken());

    }

    /**
     * get Variants for team, used to provide stats upgrade
     * Current:
     * maxEnergy| max Energy increasement
     * pmaxEnergy| max Energy multiplier
     * generator_loc| generator location, to keep generators unique.
     *
     * @return variants<br>
     */
    public CompoundTag getVariants() {
        return variants;
    }

    /**
     * Grant effect to the team, optionally to a player. Sending packets and run {@link Effect#grant(TeamResearchData, net.minecraft.entity.player.PlayerEntity, boolean)}
     *
     * @param e      the e<br>
     * @param player the player, only useful when player manually click "claim awards" or do similar things.<br>
     */
    public void grantEffect(TeamDataHolder team,Research r,Effect e, @Nullable ServerPlayer player) {
        if (!this.isEffectGranted(r, e)) {
        	e.grant(team,this, player, false);
            this.setEffectGranted(r, e, true);
            sendEffectProgressPacket(team,r,r.getEffects().indexOf(e),true);
        }
    }

    public void sendVariantPacket(TeamDataHolder holder) {
    	FHResearchAttributeSyncPacket pack=new FHResearchAttributeSyncPacket(variants);
    	holder.sendToOnline(pack);
    }
    public boolean hasVariant(ResearchVariant name) {
        return variants.contains(name.getToken());
    }


    public void putVariantDouble(ResearchVariant name, double val) {
        variants.putDouble(name.getToken(), val);
    }

    public void putVariantLong(ResearchVariant name, long val) {
        variants.putLong(name.getToken(), val);
    }
/*d
    public void reload() {
        crafting.reload();
        building.reload();
        sendUpdate();
    }*/

    public void removeVariant(ResearchVariant name) {
        variants.remove(name.getToken());
    }

    /**
     * Reset data.
     *
     * @param r the r<br>
     */
    public void resetData(TeamDataHolder team,Research r) {
    	ResearchData rd=getData(r);
    	rd.reset();

    	/*int i=0;
        for (Clue c : r.getClues()) {
        	if(team!=null) {
        		c.end(team, r);
                sendClueProgressPacket(team,r, i++, false);
        	}
        }
        i=0;
        for (Effect e : r.getEffects()) {
            //e.revoke(this);
            if (team!=null) {
            	sendEffectProgressPacket(team,r, i++, false);
            }
        }*/
        if (team!=null) {
        	for (Clue c : r.getClues()) {
            	c.end(team, r);
            }
            this.sendResearchProgressPacket(team, r);
        }
        
    }

    public void sendUpdate(TeamDataHolder team) {
        FHResearchDataSyncPacket packet = new FHResearchDataSyncPacket(this);
        team.sendToOnline(packet);
 
    }



    @OnlyIn(Dist.CLIENT)
    public void setCurrentResearch(int id) {
    	this.activeResearchId=id;
    }
    /**
     * set current research.
     *
     * @param r value to set current research to.
     */
    public void setCurrentResearch(TeamDataHolder team,Research r) {
        ResearchData rd = this.getData(r);
        int index=FHResearch.researches.getIntId(r);
        if (rd.active && !rd.finished) {
            if (this.activeResearchId != index) {
                if (this.activeResearchId != 0)
                    clearCurrentResearch(team,false);
                this.activeResearchId = index;
                FHChangeActiveResearchPacket packet = new FHChangeActiveResearchPacket(r);
                team.sendToOnline(packet);
                for (Clue c : r.getClues())
                    c.start(team,r);
                
                this.checkResearchComplete(team,r);
            }
        }
    }


	public void setVariants(CompoundTag variants) {
		this.variants = variants;
	}

    public boolean setInsight(int insight) {
        this.insight = insight;
        int newLevel = computeLevelFromInsight(this.insight);
        if (newLevel != insightLevel) {
            insightLevel = newLevel;
            return true;
        }
        return false;
    }

    public void setInsightOnly(int insight) {
        this.insight = insight;
    }

    public int getInsight() {
        return insight;
    }

    public boolean setInsightLevel(int insightLevel) {
        this.insightLevel = insightLevel;
        int newInsight = computeInsightFromLevel(insightLevel);
        if (newInsight != insight) {
            insight = newInsight;
            return true;
        }
        return false;
    }

    public void setInsightLevelOnly(int insightLevel) {
        this.insightLevel = insightLevel;
    }

    public int getInsightLevel() {
        return insightLevel;
    }

    public boolean setUsedInsightLevel(int usedInsightLevel) {
        if (usedInsightLevel > insightLevel) {
            FHMain.LOGGER.warn("Used insights level cannot exceed insights level. Set to insights level instead.");
            this.usedInsightLevel = insightLevel;
            return false;
        }
        this.usedInsightLevel = usedInsightLevel;
        return true;
    }

    public int getUsedInsightLevel() {
        return usedInsightLevel;
    }

    /**
     * Used to compute the insights level from insights point.
     * TODO: The function is placeholder. Should be adjusted.
     * @param insights insights point
     */
    public static final double GROWTH_RATE_MULTIPLIER = 0.5;
    public static int computeLevelFromInsight(int insights) {
        return (int) Math.floor(Math.sqrt(GROWTH_RATE_MULTIPLIER * insights + 9) - 3);
    }
    public static int computeInsightFromLevel(int level) {
        return (int) Math.ceil( ((level + 3) * (level + 3) - 9) / GROWTH_RATE_MULTIPLIER);
    }

    /**
     * Add insights point to the team.
     * Increment level if needed.
     * @param newInsights insights point to add
     * @return whether the level is changed.
     */
    public boolean addInsight(int newInsights) {
        this.insight += newInsights;
        int newLevel = computeLevelFromInsight(this.insight);
        if (newLevel != insightLevel) {
            insightLevel = newLevel;
            return true;
        }
        return false;
    }

    /**
     * Get the fraction of how much insights point is needed to reach next level.
     * @return a float in [0, 1)
     *
     * TODO: Fix somehow insight is always synced to the currentLevelInsights
     */
    public float getInsightProgress() {
        int nextLevel = insightLevel + 1;
        int nextLevelInsights = computeInsightFromLevel(nextLevel);
        int currentLevelInsights = computeInsightFromLevel(insightLevel);
        return (float) (insight - currentLevelInsights) / (nextLevelInsights - currentLevelInsights);
    }

    /**
     * Get available insights level.
     * @return available insights level
     */
    public int getAvailableInsightLevel() {
        int level = insightLevel - usedInsightLevel;
        if (level < 0) {
            FHMain.LOGGER.warn("Used insights level exceeds insights level.");
            level = 0;
        }
        return level;
    }

	public static final Codec<TeamResearchData> CODEC=RecordCodecBuilder.create(t->
	t.group(
		    CompoundTag.CODEC.fieldOf("vars").forGetter(o->o.variants),
		    Codec.list(ResearchData.CODEC).fieldOf("researches").forGetter(o->o.rdata),
            CodecUtil.defaultValue(Codec.INT, 0).fieldOf("active").forGetter(o->o.activeResearchId),
            CodecUtil.defaultValue(Codec.INT, 0).fieldOf("insight").forGetter(o->o.insight),
            CodecUtil.defaultValue(Codec.INT, 0).fieldOf("insightLevel").forGetter(o->o.insightLevel),
            CodecUtil.defaultValue(Codec.INT, 0).fieldOf("usedInsightLevel").forGetter(o->o.usedInsightLevel)
//            Codec.INT.fieldOf("active").forGetter(o->o.activeResearchId),
//            Codec.INT.fieldOf("insight").forGetter(o->o.insight),
//            Codec.INT.fieldOf("insightLevel").forGetter(o->o.insightLevel),
//            Codec.INT.fieldOf("usedInsightLevel").forGetter(o->o.usedInsightLevel)
		).apply(t, TeamResearchData::new));

	boolean isInited;
	public TeamResearchData( CompoundTag variants, List<ResearchData> rdata, int activeResearchId, int insight, int insightLevel, int usedInsightLevel) {
		super();
        crafting.clear();
        building.clear();
        block.clear();
        categories.clear();
		this.rdata.addAll(rdata);
		this.activeResearchId = activeResearchId;
        this.insight = insight;
        this.insightLevel = insightLevel;
        this.usedInsightLevel = usedInsightLevel;
		this.variants = variants;

	}

	public void initResearch(TeamDataHolder team) {
        for (Research r:FHResearch.getAllResearch()) {
            ResearchData rd=getData(r);
            if(rd.isCompleted()) {
	            for(Effect e:r.getEffects())
	                 e.grant(team,this, null, true);
            }
        }
        
        if (activeResearchId != 0) {
            Research r = FHResearch.researches.getById(activeResearchId);
           
            for (Clue c : r.getClues())
                c.start(team,r);
            
        }
	}
    public void putVariantDouble(String name, double val) {
        variants.putDouble(name, val);
    }

    public void putVariantLong(String name, long val) {
        variants.putLong(name, val);
    }


}
