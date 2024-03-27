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
import com.teammoeg.frostedheart.base.team.SpecialData;
import com.teammoeg.frostedheart.base.team.SpecialDataHolder;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.ResearchListeners.BlockUnlockList;
import com.teammoeg.frostedheart.content.research.ResearchListeners.CategoryUnlockList;
import com.teammoeg.frostedheart.content.research.ResearchListeners.MultiblockUnlockList;
import com.teammoeg.frostedheart.content.research.ResearchListeners.RecipeUnlockList;
import com.teammoeg.frostedheart.content.research.network.FHChangeActiveResearchPacket;
import com.teammoeg.frostedheart.content.research.network.FHResearchAttributeSyncPacket;
import com.teammoeg.frostedheart.content.research.network.FHResearchDataSyncPacket;
import com.teammoeg.frostedheart.content.research.network.FHResearchDataUpdatePacket;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.content.research.research.clues.Clue;
import com.teammoeg.frostedheart.content.research.research.effects.Effect;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import com.teammoeg.frostedheart.util.utility.OptionalLazy;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// TODO: Auto-generated Javadoc

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
     * The clue complete.<br>
     */
    BitSet clueComplete = new BitSet();

    /**
     * The granted effects.<br>
     */
    BitSet grantedEffects = new BitSet();

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
    CompoundNBT variants = new CompoundNBT();

    

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

    TeamDataHolder holder;
    public TeamResearchData(SpecialDataHolder team) {
    	if(team instanceof TeamDataHolder)
    		holder=(TeamDataHolder) team;
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

    /**
     * Clear current research.
     *
     * @param sync send update packet
     */
    public void clearCurrentResearch(boolean sync) {
        if (activeResearchId == 0) return;
        Research r = FHResearch.researches.getById(activeResearchId);
        if (r != null) {
			for (Clue c : r.getClues())
				c.end(getHolder());
        }
        activeResearchId = 0;
        if (sync) {
            FHChangeActiveResearchPacket packet = new FHChangeActiveResearchPacket();
            getHolder().sendToOnline(packet);
        }
    }

    /**
     * Clear current research.
     *
     * @param r the r<br>
     */
    public void clearCurrentResearch(Research r) {
        if (activeResearchId == FHResearch.researches.getIntId(r))
            clearCurrentResearch(true);
    }

    public void clearData(Research r) {
    	int index=FHResearch.researches.getIntId(r);
        if (index <= this.rdata.size()) {
            this.rdata.set(index - 1, null);
            for (Clue c : r.getClues()) {
                this.setClueTriggered(c, false);
                c.sendProgressPacket(holder);
            }
            for (Effect e : r.getEffects()) {
                this.setGrant(e, false);
                e.sendProgressPacket(holder);
            }
        }
    }


    /**
     * Commit research points to current research.<br>
     *
     * @param points the points<br>
     * @return unused points after commit to current research.
     */
    public long doResearch(long points) {
        OptionalLazy<Research> rs = getCurrentResearch();
        if (rs.isPresent()) {
            Research r = rs.resolve().get();
            ResearchData rd = this.getData(r);
            long remain = rd.commitPoints(points);
            rd.sendProgressPacket();
            return remain;
        }
        return points;
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
        return OptionalLazy.of(() -> FHResearch.getResearch(activeResearchId).get());
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
            rnd = new ResearchData(FHResearch.getResearch(id), this);
            rdata.set(id - 1, rnd);
        }
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
    public CompoundNBT getVariants() {
        return variants;
    }

    /**
     * Grant effect to the team, optionally to a player. Sending packets and run {@link Effect#grant(TeamResearchData, net.minecraft.entity.player.PlayerEntity, boolean)}
     *
     * @param e      the e<br>
     * @param player the player, only useful when player manually click "claim awards" or do similar things.<br>
     */
    public void grantEffect(Effect e, @Nullable ServerPlayerEntity player) {
        int id = FHResearch.effects.getIntId(e);
        ensureEffect(id);
        if (id > 0)
            if (!grantedEffects.get(id - 1)) {
                grantedEffects.set(id - 1, e.grant(this, player, false));
                e.sendProgressPacket(holder);
            }
    }

    public void sendVariantPacket() {
    	FHResearchAttributeSyncPacket pack=new FHResearchAttributeSyncPacket(variants);
    	holder.sendToOnline(pack);
    }
    public boolean hasVariant(ResearchVariant name) {
        return variants.contains(name.getToken());
    }

    /**
     * Checks if is clue triggered.<br>
     *
     * @param clue the clue<br>
     * @return if is clue triggered,true.
     */
    public boolean isClueTriggered(Clue clue) {
        return isClueTriggered(FHResearch.clues.getIntId(clue));
    }

    /**
     * Checks if clue is triggered.<br>
     *
     * @param id the id<br>
     * @return if is clue triggered,true.
     */
    public boolean isClueTriggered(int id) {
        if (clueComplete.size() >= id && id > 0) {
            Boolean b = clueComplete.get(id - 1);
            return b != null && b;
        }
        return false;
    }

    /**
     * Checks if is clue triggered.<br>
     *
     * @param lid the lid<br>
     * @return if is clue triggered,true.
     */
    public boolean isClueTriggered(String lid) {
        return isClueTriggered(FHResearch.clues.getByName(lid));
    }

    /**
     * Checks if is effect granted.<br>
     *
     * @param e the e<br>
     * @return if is effect granted,true.
     */
    public boolean isEffectGranted(Effect e) {
        return isEffectGranted(FHResearch.effects.getIntId(e));
    }

    /**
     * Checks if effect is granted.<br>
     *
     * @param id the id<br>
     * @return if is effect granted,true.
     */
    public boolean isEffectGranted(int id) {
        if (grantedEffects.size() >= id && id > 0) {
            return grantedEffects.get(id - 1);
        }
        return false;
    }

    public void putVariantDouble(ResearchVariant name, double val) {
        variants.putDouble(name.getToken(), val);
    }

    public void putVariantLong(ResearchVariant name, long val) {
        variants.putLong(name.getToken(), val);
    }

    public void reload() {
        crafting.reload();
        building.reload();
        sendUpdate();
    }

    public void removeVariant(ResearchVariant name) {
        variants.remove(name.getToken());
    }

    /**
     * Reset data.
     *
     * @param r the r<br>
     */
    public void resetData(Research r, boolean causeUpdate) {
    	int index=FHResearch.researches.getIntId(r);
        if (index <= this.rdata.size()) {
            this.rdata.set(index - 1, null);
            for (Clue c : r.getClues()) {
                this.setClueTriggered(c, false);
                if (causeUpdate)
                    c.sendProgressPacket(holder);
            }
            for (Effect e : r.getEffects()) {
                this.setGrant(e, false);
                e.revoke(this);
                if (causeUpdate) {
                    e.sendProgressPacket(holder);
                }
            }
            if (causeUpdate) {
                FHResearchDataUpdatePacket packet = new FHResearchDataUpdatePacket(index);
                holder.sendToOnline(packet);
            }
        }
    }

    public void sendUpdate() {
        FHResearchDataSyncPacket packet = new FHResearchDataSyncPacket(this);
        holder.sendToOnline(packet);
 
    }


    /**
     * Sets the clue triggered.
     *
     * @param clue the clue<br>
     * @param trig the trig<br>
     */
    public void setClueTriggered(Clue clue, boolean trig) {
        setClueTriggered(FHResearch.clues.getIntId(clue), trig);
    }

    /**
     * Set clue is triggered, this would send packets and check current research's completion status if completed.
     *
     * @param id   the number id<br>
     * @param trig new trigger status<br>
     */
    public void setClueTriggered(int id, boolean trig) {
        ensureClue(id);
        if (id > 0) {
            clueComplete.set(id - 1, trig);
            getCurrentResearch().ifPresent(r -> this.getData(r).checkComplete());
        }
    }

    /**
     * Sets the clue triggered.
     *
     * @param lid  the lid<br>
     * @param trig the trig<br>
     */
    public void setClueTriggered(String lid, boolean trig) {
        setClueTriggered(FHResearch.clues.getByName(lid), trig);
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
    public void setCurrentResearch(Research r) {
        ResearchData rd = this.getData(r);
        int index=FHResearch.researches.getIntId(r);
        if (rd.active && !rd.finished) {
            if (this.activeResearchId != index) {
                if (this.activeResearchId != 0)
                    clearCurrentResearch(false);
                this.activeResearchId = index;
                FHChangeActiveResearchPacket packet = new FHChangeActiveResearchPacket(r);
                holder.sendToOnline(packet);
                for (Clue c : r.getClues())
                    c.start(holder);
                
                this.getData(r).checkComplete();
            }
        }
    }

    /**
     * Set effect granted state.
     * This would not send packet, mostly for client use.
     * See {@link #grantEffect(Effect, ServerPlayerEntity) for effect granting.}
     *
     * @param e    the e<br>
     * @param flag operation flag
     */
    public void setGrant(Effect e, boolean flag) {
        int id = FHResearch.effects.getIntId(e);
        ensureEffect(id);
        grantedEffects.set(id - 1, flag);

    }



    /**
     * Trigger clue, similar to {@link #triggerClue(int)} but accept Clue.
     *
     * @param clue the clue<br>
     */
    public void triggerClue(Clue clue) {
        triggerClue(FHResearch.clues.getIntId(clue));
    }

    /**
     * Trigger clue, this would send packets and check current research's completion status if completed.
     *
     * @param id the number id<br>
     */
    public void triggerClue(int id) {
        setClueTriggered(id, true);
    }

    /**
     * Trigger clue, similar to {@link #triggerClue(int)} but accept its string name.
     *
     * @param lid the lid<br>
     */
    public void triggerClue(String lid) {
        triggerClue(FHResearch.clues.getByName(lid));
    }

	public void setVariants(CompoundNBT variants) {
		this.variants = variants;
	}
	public static final Codec<TeamResearchData> CODEC=RecordCodecBuilder.create(t->
	t.group(CodecUtil.LONG_ARRAY_CODEC.fieldOf("clues").forGetter(o->o.clueComplete.toLongArray()),
		CodecUtil.LONG_ARRAY_CODEC.fieldOf("effects").forGetter(o->o.grantedEffects.toLongArray()),
		CompoundNBT.CODEC.fieldOf("vars").forGetter(o->o.variants),
		Codec.list(ResearchData.CODEC).fieldOf("researches").forGetter(o->o.rdata),
		Codec.INT.fieldOf("active").forGetter(o->o.activeResearchId)
		).apply(t, TeamResearchData::new));
	boolean isInited;
	public TeamResearchData(long[] clueComplete, long[] grantedEffects, CompoundNBT variants, List<ResearchData> rdata, int activeResearchId) {
		super();
        crafting.clear();
        building.clear();
        block.clear();
        categories.clear();
		this.clueComplete = BitSet.valueOf(clueComplete);
		this.grantedEffects = BitSet.valueOf(grantedEffects);
		this.rdata.addAll(rdata);
        for (int i = 0; i < rdata.size(); i++) {
            rdata.get(i).setParent(FHResearch.getResearch(i + 1), this);
        }
		this.activeResearchId = activeResearchId;
		this.variants = variants;
        for (int i = 0; i < this.grantedEffects.length(); i++) {
            if (this.grantedEffects.get(i))
                FHResearch.effects.runIfPresent(i + 1, e -> e.grant(this, null, true));
        }
        
        if (activeResearchId != 0) {
            Research r = FHResearch.researches.getById(activeResearchId);
           
            for (Clue c : r.getClues())
                c.start(holder);
            
        }
	}

	public TeamDataHolder getHolder() {
		return holder;
	}
	public UUID getId() {
		return holder.getId();
	}

    public void putVariantDouble(String name, double val) {
        variants.putDouble(name, val);
    }

    public void putVariantLong(String name, long val) {
        variants.putLong(name, val);
    }

	@Override
	public void setHolder(SpecialDataHolder holder) {
		this.holder=(TeamDataHolder) holder;
	}
}
