/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedresearch.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataHolder;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.util.RecipeUtils;
import com.teammoeg.chorda.util.struct.OptionalLazy;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.ResearchListeners.BlockUnlockList;
import com.teammoeg.frostedresearch.ResearchListeners.CategoryUnlockList;
import com.teammoeg.frostedresearch.ResearchListeners.MultiblockUnlockList;
import com.teammoeg.frostedresearch.ResearchListeners.RecipeUnlockList;
import com.teammoeg.frostedresearch.events.ResearchStatusEvent;
import com.teammoeg.frostedresearch.network.*;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.clues.Clue;
import com.teammoeg.frostedresearch.research.effects.Effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class TeamResearchData.
 * <p>
 * This stores all data for research, restrictions, locks, clues and points
 * data. This data is available for both team and player.
 *
 * @author khjxiaogu
 * @date 2022/9/2
 */
public class TeamResearchData implements SpecialData {

	public static final double GROWTH_RATE_MULTIPLIER = 0.5;
	public static final Codec<TeamResearchData> CODEC = RecordCodecBuilder.create(t -> t.group(
		CompoundTag.CODEC.fieldOf("vars").forGetter(o -> o.variants),
		Codec.unboundedMap(Codec.STRING, ResearchData.CODEC).fieldOf("researches").forGetter(o -> o.rdata),
		Codec.INT.optionalFieldOf("active",-1).forGetter(o -> o.activeResearchId),
		Codec.INT.optionalFieldOf("insight",0).forGetter(o -> o.insight),
		Codec.INT.optionalFieldOf("insightLevel",0).forGetter(o -> o.insightLevel),
		Codec.INT.optionalFieldOf("usedInsightLevel",0).forGetter(o -> o.usedInsightLevel)// ,
	// BlockUnlockList.CODEC.optionalFieldOf("blockUnlockList", new
	// BlockUnlockList()).forGetter(o -> o.block),
	// RecipeUnlockList.CODEC.optionalFieldOf("recipeUnlockList", new
	// RecipeUnlockList()).forGetter(o -> o.crafting),
	// MultiblockUnlockList.CODEC.optionalFieldOf("multiblockUnlockList", new
	// MultiblockUnlockList()).forGetter(o -> o.building),
	// CategoryUnlockList.CODEC.optionalFieldOf("categoryUnlockList", new
	// CategoryUnlockList()).forGetter(o -> o.categories)
	).apply(t, TeamResearchData::new));
	public static final Codec<TeamResearchData> NETWORK_CODEC = RecordCodecBuilder.create(t -> t.group(
		CompoundTag.CODEC.fieldOf("vars").forGetter(o -> o.variants),
		CodecUtil.discreteList(ResearchData.CODEC).fieldOf("researches").forGetter(o -> FHResearch.researches.toList(o.rdata)),
		Codec.INT.optionalFieldOf("active",-1).forGetter(o -> o.activeResearchId),
		Codec.INT.optionalFieldOf("insight",0).forGetter(o -> o.insight),
		Codec.INT.optionalFieldOf("insightLevel",0).forGetter(o -> o.insightLevel),
		Codec.INT.optionalFieldOf("usedInsightLevel",0).forGetter(o -> o.usedInsightLevel)// ,
	// BlockUnlockList.CODEC.optionalFieldOf("blockUnlockList", new
	// BlockUnlockList()).forGetter(o -> o.block),
	// RecipeUnlockList.CODEC.optionalFieldOf("recipeUnlockList", new
	// RecipeUnlockList()).forGetter(o -> o.crafting),
	// MultiblockUnlockList.CODEC.optionalFieldOf("multiblockUnlockList", new
	// MultiblockUnlockList()).forGetter(o -> o.building),
	// CategoryUnlockList.CODEC.optionalFieldOf("categoryUnlockList", new
	// CategoryUnlockList()).forGetter(o -> o.categories)
	).apply(t, TeamResearchData::new));
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
	/**
	 * The Insights point.<br>
	 * <p>
	 * Insights are rewarded by active actions and new discoveries.
	 * <p>
	 * Insights is monotone increasing.
	 */
	int insight = 0;
	/**
	 * The Insights level.<br>
	 * <p>
	 * Computed from insights point.
	 * <p>
	 * Insights level is a monotone increasing function of insights point.
	 */
	int insightLevel = 0;
	/**
	 * The used insights level.<br>
	 * <p>
	 * Completing one research would increment this value.
	 */
	int usedInsightLevel = 0;
	/**
	 * The rdata.<br>
	 */
	Map<String, ResearchData> rdata = new HashMap<>();
	/**
	 * The active research id.<br>
	 */
	int activeResearchId = -1;
	/**
	 * The variants.<br>
	 */
	CompoundTag variants = new CompoundTag();
	boolean isInited;;

	public TeamResearchData(SpecialDataHolder team) {
	}

	public TeamResearchData(CompoundTag variants, Map<String, ResearchData> rdata, int activeResearchId,
		int insight, int insightLevel, int usedInsightLevel) {
		super();
		this.rdata.clear();
		this.rdata.putAll(rdata);
		this.activeResearchId = activeResearchId;
		this.insight = insight;
		this.insightLevel = insightLevel;
		this.usedInsightLevel = usedInsightLevel;
		this.variants = variants;

	}

	public TeamResearchData(CompoundTag variants, List<ResearchData> rdata, int activeResearchId,
		int insight, int insightLevel, int usedInsightLevel) {
		super();
		this.rdata.clear();
		FHResearch.researches.fromList(rdata, this.rdata::put);
		this.activeResearchId = activeResearchId;
		this.insight = insight;
		this.insightLevel = insightLevel;
		this.usedInsightLevel = usedInsightLevel;
		this.variants = variants;

	}

	/**
	 * Used to compute the insights level from insights point. TODO: The function is
	 * placeholder. Should be adjusted.
	 *
	 * @param insights insights point
	 */
	public static int computeLevelFromInsight(int insights) {
		return (int) Math.floor(Math.sqrt(GROWTH_RATE_MULTIPLIER * insights + 9) - 3);
	}

	public static int computeInsightFromLevel(int level) {
		return (int) Math.ceil(((level + 3) * (level + 3) - 9) / GROWTH_RATE_MULTIPLIER);
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

	public void setHolder(SpecialDataHolder holder) {
	}

	/**
	 * Clear current research.
	 *
	 * @param sync send update packet
	 */
	public void clearCurrentResearch(TeamDataHolder team, boolean sync) {
		if (activeResearchId == -1) return;
		Research r = FHResearch.researches.get(activeResearchId);
		activeResearchId = -1;
		if (r != null)
			if (team != null) {
				for (Clue c : r.getClues())
					c.end(team, r);
				if (sync)
					team.sendToOnline(FRNetwork.INSTANCE,new FHChangeActiveResearchPacket());
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
			activeResearchId = -1;
	}

	/**
	 * Commit research points to current research.<br>
	 *
	 * @param points the points<br>
	 * @return unused points after commit to current research.
	 */
	public long doResearch(TeamDataHolder team, long points) {
		OptionalLazy<Research> rs = getCurrentResearch();
		if (rs.isPresent()) {
			Research r = rs.resolve().get();
			ResearchData rd = this.getData(r);
			if (!rd.active || rd.finished)
				return points;
			return rd.commitPoints(r, points, () -> {
				checkResearchComplete(team, r);
				sendResearchProgressPacket(team, r);
			});
		}
		return points;
	}

	public boolean commitItem(ServerPlayer player, TeamDataHolder team, Research research) {
		if (research.isInCompletable()) return false;
		for (Research par : research.getParents()) {
			if (!getData(par).isCompleted()) {
				return false;
			}
		}
		if (!this.hasInsight(research.getInsight()))
			return false;
		if (!research.getRequiredItems().isEmpty() && !RecipeUtils.costItems(player, research.getRequiredItems()))
			return false;
		this.costInsight(team,research.getInsight());
		getData(research).setActive();
		this.sendResearchProgressPacket(team, research);
		return true;
	}

	public boolean isClueCompleted(Research par, Clue clue) {
		return getData(par).isClueTriggered(clue);
	}

	public void setClueCompleted(TeamDataHolder team, Research par, int clue, boolean trig) {

		Clue cl = par.getClues().get(clue);
		if (cl != null) {
			if (isClueCompleted(par, cl) == trig) return;
			getData(par).setClueTriggered(cl.getNonce(), trig);
			if (trig)
				cl.end(team, par);
			else
				cl.start(team, par);
			sendClueProgressPacket(team, par, clue, trig);
		}
	}

	public void setClueCompleted(TeamDataHolder team, Research par, Clue clue, boolean trig) {
		if (isClueCompleted(par, clue) == trig) return;
		getData(par).setClueTriggered(clue.getNonce(), trig);
		if (trig)
			clue.end(team, par);
		else
			clue.start(team, par);
		sendClueProgressPacket(team, par, par.getClues().indexOf(clue), trig);
	}

	public boolean isEffectGranted(Research research, Effect e) {
		return getData(research).isEffectGranted(e);
	}

	public void setEffectGranted(Research rch, Effect effect, boolean b) {

		getData(rch).setEffectGranted(effect, b);

	}

	public boolean checkResearchComplete(TeamDataHolder team, Research r) {
		ResearchData rd = getData(r);
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

			this.annouceResearchComplete(team, r);
			return true;
		}
		return false;
	}

	public void setResearchFinished(TeamDataHolder team, Research rs, boolean data) {
		ResearchData rd = getData(rs);
		// System.out.println(rs);
		rd.setFinished(data);

		if (data) {
			this.grantEffects(team, null, rs);
			this.sendResearchProgressPacket(team, rs);
			this.annouceResearchComplete(team, rs);
		}
	}

	/**
	 * Actually grants the Research's active Effects to the team's player.
	 * <p>
	 * This must be called after an Effect is set to active (true).
	 *
	 * @param spe    the Player to grant the effects to
	 * @param rs     the Research to grant the effects of
	 * @param holder the TeamDataHolder
	 */
	public void grantEffects(TeamDataHolder holder, ServerPlayer spe, Research rs) {
		boolean granted = true;
		ResearchData rd = getData(rs);
		for (Effect e : rs.getEffects()) {
			grantEffect(holder, rs, e, spe);
			granted &= rd.isEffectGranted(e);
		}
		if (rs.isInfinite() && granted) {
			int lvl = rd.getLevel();
			this.resetData(holder, rs);
			rd.setLevel(lvl + 1);
		}

	}

	public void grantAllEffects(TeamDataHolder holder) {
		for (Research rs : FHResearch.getAllResearch()) {
			grantEffects(holder, null, rs);
		}
	}

	private void sendClueProgressPacket(TeamDataHolder team, Research par, int clue, boolean trig) {
		team.sendToOnline(FRNetwork.INSTANCE,new FHS2CClueProgressSyncPacket(trig, FHResearch.researches.getIntId(par), clue));
	}

	/**
	 * Send effect progress packet for current effect to players in team. Useful for
	 * data sync. This would called automatically, Their's no need to call this in
	 * effect.
	 *
	 * @param team the team<br>
	 */
	private void sendEffectProgressPacket(TeamDataHolder team, Research par, int effect, boolean trig) {
		FHEffectProgressSyncPacket packet = new FHEffectProgressSyncPacket(trig, FHResearch.researches.getIntId(par), effect);
		team.sendToOnline(FRNetwork.INSTANCE,packet);
	}

	private void annouceResearchComplete(TeamDataHolder team, Research par) {
		team.getTeam().ifPresent(e -> MinecraftForge.EVENT_BUS.post(new ResearchStatusEvent(par, e, getData(par).finished)));
	}

	private void sendResearchProgressPacket(TeamDataHolder team, Research par) {
		team.sendToOnline(FRNetwork.INSTANCE,new FHResearchDataUpdatePacket(par, getData(par)));
	}

	private void sendInsightChangePacket(TeamDataHolder team) {
		team.sendToOnline(FRNetwork.INSTANCE,new FHInsightSyncPacket(insight, insightLevel, usedInsightLevel));
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
	/*
	 * public void ensureResearch(int len) { rdata.ensureCapacity(len + 1); while
	 * (rdata.size() < len + 1) rdata.add(null); }
	 */

	/**
	 * Get current research.
	 *
	 * @return current research<br>
	 */
	public OptionalLazy<Research> getCurrentResearch() {
		if (activeResearchId == -1)
			return OptionalLazy.empty();
		return OptionalLazy.of(() -> FHResearch.getResearch(activeResearchId));
	}

	@OnlyIn(Dist.CLIENT)
	public void setCurrentResearch(int id) {
		this.activeResearchId = id;
	}

	/**
	 * Get research data.
	 *
	 * @param id the id<br>
	 * @return data<br>
	 */
	public ResearchData getData(String id) {
		ResearchData rnd = rdata.get(id);
		if (rnd == null) {
			rnd = new ResearchData();
			rdata.put(id, rnd);
		}
		return rnd;
	}

	/**
	 * Get research data.
	 *
	 * @param id the id<br>
	 * @return data<br>
	 */
	public ResearchData getData(int id) {
		return getData(FHResearch.researches.get(id));
	}

	public ResearchData setData(String id, ResearchData rd) {
		ResearchData rnd = rdata.put(id, rd);
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
		return getData(rs.getId());
	}

	public double getVariantDouble(ResearchVariant name) {
		return variants.getDouble(name.getToken());

	}
	/*
	 * d public void reload() { crafting.reload(); building.reload(); sendUpdate();
	 * }
	 */

	public long getVariantLong(ResearchVariant name) {
		return variants.getLong(name.getToken());

	}

	/**
	 * get Variants for team, used to provide stats upgrade Current: maxEnergy| max
	 * Energy increasement pmaxEnergy| max Energy multiplier generator_loc|
	 * generator location, to keep generators unique.
	 *
	 * @return variants<br>
	 */
	public CompoundTag getVariants() {
		return variants;
	}

	public void setVariants(CompoundTag variants) {
		this.variants = variants;
	}

	/**
	 * Grant effect to the team, optionally to a player. Sending packets and run
	 * {@link Effect#grant(TeamResearchData, net.minecraft.entity.player.PlayerEntity, boolean)}
	 *
	 * @param e      the e<br>
	 * @param player the player, only useful when player manually click "claim
	 *               awards" or do similar things.<br>
	 */
	public void grantEffect(TeamDataHolder team, Research r, Effect e, @Nullable ServerPlayer player) {
		if (!this.isEffectGranted(r, e)) {
			e.grant(team, this, player, false);
			this.setEffectGranted(r, e, true);
			sendEffectProgressPacket(team, r, r.getEffects().indexOf(e), true);
		}
	}

	public void sendVariantPacket(TeamDataHolder holder) {
		FHResearchAttributeSyncPacket pack = new FHResearchAttributeSyncPacket(variants);
		holder.sendToOnline(FRNetwork.INSTANCE,pack);
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

	public void removeVariant(ResearchVariant name) {
		variants.remove(name.getToken());
	}

	/**
	 * Reset data.
	 *
	 * @param r the r<br>
	 */
	public void resetData(TeamDataHolder team, Research r) {
		ResearchData rd = getData(r);
		rd.reset();

		/*
		 * int i=0; for (Clue c : r.getClues()) { if(team!=null) { c.end(team, r);
		 * sendClueProgressPacket(team,r, i++, false); } } i=0; for (Effect e :
		 * r.getEffects()) { //e.revoke(this); if (team!=null) {
		 * sendEffectProgressPacket(team,r, i++, false); } }
		 */
		if (team != null) {
			for (Clue c : r.getClues()) {
				c.end(team, r);
			}
			this.sendResearchProgressPacket(team, r);
		}

	}

	public void sendUpdate(TeamDataHolder team) {
		FHResearchDataSyncPacket packet = new FHResearchDataSyncPacket(this);
		team.sendToOnline(FRNetwork.INSTANCE,packet);

	}

	/**
	 * set current research.
	 *
	 * @param r value to set current research to.
	 */
	public void setCurrentResearch(TeamDataHolder team, Research r) {
		ResearchData rd = this.getData(r);
		int index = FHResearch.researches.getIntId(r);
		if (rd.active && !rd.finished) {
			if (this.activeResearchId != index) {
				if (this.activeResearchId != -1)
					clearCurrentResearch(team, false);
				this.activeResearchId = index;
				FHChangeActiveResearchPacket packet = new FHChangeActiveResearchPacket(r);
				team.sendToOnline(FRNetwork.INSTANCE,packet);
				for (Clue c : r.getClues())
					c.start(team, r);

				this.checkResearchComplete(team, r);
			}
		}
	}

	public boolean setInsight(TeamDataHolder team, int insight) {
		this.insight = insight;
		int newLevel = computeLevelFromInsight(this.insight);
		boolean updateedLevel = newLevel != insightLevel;
		insightLevel = newLevel;
		if(team!=null)
			sendInsightChangePacket(team);
		return updateedLevel;
	}

	public int getInsight() {
		return insight;
	}

	public boolean setInsightLevel(TeamDataHolder team,int insightLevel) {
		this.insightLevel = insightLevel;
		int newInsight = computeInsightFromLevel(insightLevel);
		boolean updateed = newInsight != insight;
		insight = newInsight;
		if(team!=null)
			sendInsightChangePacket(team);
		return updateed;
	}

	public void updateInsight(int insight,int insightLevel,int usedInsight) {
		this.insight=insight;
		this.insightLevel = insightLevel;
		this.usedInsightLevel=usedInsight;
	}

	public int getInsightLevel() {
		return insightLevel;
	}

	public boolean setUsedInsightLevel(TeamDataHolder team,int usedInsightLevel) {
		if (usedInsightLevel > insightLevel) {
			FRMain.LOGGER.warn("Used insights level cannot exceed insights level. Set to insights level instead.");
			this.usedInsightLevel = insightLevel;
			sendInsightChangePacket(team);
			return false;
		}
		this.usedInsightLevel = usedInsightLevel;
		sendInsightChangePacket(team);
		return true;
	}

	public int getUsedInsightLevel() {
		return usedInsightLevel;
	}

	/**
	 * Add insights point to the team. Increment level if needed.
	 *
	 * @param newInsights insights point to add
	 * @return whether the level is changed.
	 */
	public boolean addInsight(TeamDataHolder team,int newInsights) {
		this.insight += newInsights;
		int newLevel = computeLevelFromInsight(this.insight);
		boolean updateedLevel = newLevel != insightLevel;
		insightLevel = newLevel;
		if(team!=null)
			sendInsightChangePacket(team);
		return updateedLevel;
	}

	/**
	 * Get the fraction of how much insights point is needed to reach next level.
	 *
	 * @return a float in [0, 1)
	 *         <p>
	 *         TODO: Fix somehow insight is always synced to the
	 *         currentLevelInsights
	 */
	public float getInsightProgress() {
		int nextLevel = insightLevel + 1;
		int nextLevelInsights = computeInsightFromLevel(nextLevel);
		int currentLevelInsights = computeInsightFromLevel(insightLevel);
		return (float) (insight - currentLevelInsights) / (nextLevelInsights - currentLevelInsights);
	}

	public boolean costInsight(TeamDataHolder team,int insightLvl) {
		if (this.usedInsightLevel + insightLvl >= this.insightLevel) {
			this.usedInsightLevel += insightLvl;
			sendInsightChangePacket(team);
			return true;
		}
		return false;
	}

	public boolean hasInsight(int insightLvl) {
		return this.usedInsightLevel + insightLvl >= this.insightLevel;
	}

	/**
	 * Get available insights level.
	 *
	 * @return available insights level
	 */
	public int getAvailableInsightLevel() {
		int level = insightLevel - usedInsightLevel;
		if (level < 0) {
			FRMain.LOGGER.warn("Used insights level exceeds insights level.");
			level = 0;
		}
		return level;
	}

	public void initResearch(TeamDataHolder team) {
		for (Research r : FHResearch.getAllResearch()) {
			ResearchData rd = getData(r);
			if (rd.isCompleted()) {
				for (Effect e : r.getEffects())
					e.grant(team, this, null, true);
			}
		}

		if (activeResearchId != -1) {
			Research r = FHResearch.researches.get(activeResearchId);

			for (Clue c : r.getClues())
				c.start(team, r);

		}
	}

	public void putVariantDouble(String name, double val) {
		variants.putDouble(name, val);
	}

	public void putVariantLong(String name, long val) {
		variants.putLong(name, val);
	}

}
