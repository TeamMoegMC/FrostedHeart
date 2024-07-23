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

package com.teammoeg.frostedheart.content.research.research;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.FHTeamDataManager;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.research.FHRegisteredItem;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.data.ResearchData;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.gui.FHIcons;
import com.teammoeg.frostedheart.content.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.content.research.gui.FHTextUtil;
import com.teammoeg.frostedheart.content.research.network.FHResearchDataUpdatePacket;
import com.teammoeg.frostedheart.content.research.number.IResearchNumber;
import com.teammoeg.frostedheart.content.research.research.clues.Clue;
import com.teammoeg.frostedheart.content.research.research.effects.Effect;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import com.teammoeg.frostedheart.util.io.codec.BooleansCodec;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.BaseComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Only Definition of research.
 *
 * @author khjxiaogu
 */
public class Research implements FHRegisteredItem {
    public static final Codec<Research> CODEC=RecordCodecBuilder.create(t->t.group(
    	FHIcons.CODEC.fieldOf("icon").forGetter(o->o.icon),
    	ResearchCategory.CODEC.fieldOf("category").forGetter(o->o.category),
    	CodecUtil.defaultValue(Codec.list(FHResearch.researches.SUPPLIER_CODEC),Arrays.asList()).fieldOf("parents").forGetter(o->new ArrayList<>(o.parents)),
    	Codec.list(Clue.CODEC).fieldOf("clues").forGetter(o->o.clues),
    	Codec.list(CodecUtil.INGREDIENT_SIZE_CODEC).fieldOf("ingredients").forGetter(o->o.requiredItems),
    	Codec.list(Effect.CODEC).fieldOf("effects").forGetter(o->o.effects),
    	CodecUtil.defaultValue(Codec.STRING, "").fieldOf("name").forGetter(o->o.name),
    	CodecUtil.defaultValue(Codec.list(Codec.STRING),Arrays.asList()).fieldOf("desc").forGetter(o->o.desc),
    	CodecUtil.defaultValue(Codec.list(Codec.STRING),Arrays.asList()).fieldOf("descAlt").forGetter(o->o.fdesc),
    	CodecUtil.<Research>booleans("flags")
    	.flag("showAltDesc", o->o.showfdesc)
    	.flag("hideEffects", o->o.hideEffects)
    	.flag("locked", o->o.inCompletable)
    	.flag("hidden", o->o.isHidden)
    	.flag("keepShow", o->o.alwaysShow)
    	.flag("infinite", o->o.infinite).build(),
    	Codec.INT.fieldOf("points").forGetter(o->o.points)
    	).apply(t,Research::new));
    private String id;// id of this research
	/**
     * The icon for this research.<br>
     */
    FHIcon icon;

    private ResearchCategory category=ResearchCategory.RESCUE;
    private HashSet<Supplier<Research>> parents = new HashSet<>();// parent researches
    private HashSet<Supplier<Research>> children = new HashSet<>();// child researches, this is set automatically,
    // should not set manually.
    private List<Clue> clues = new ArrayList<>();// research clues

    /**
     * The required items.<br>
     */
    List<IngredientWithSize> requiredItems = new ArrayList<>();

    List<IResearchNumber> requiredItemsCountOverride = new ArrayList<>();
    private List<Effect> effects = new ArrayList<>();// effects of this research
    /**
     * The name.<br>
     */
    String name = "";

    /**
     * The desc.<br>
     */
    List<String> desc=new ArrayList<>();

    /**
     * The fdesc.<br>
     */
    List<String> fdesc = new ArrayList<>();

    /**
     * The showfdesc.<br>
     */
    boolean showfdesc;

    /**
     * The hide effects.<br>
     */
    boolean hideEffects;

    private boolean inCompletable = false;
    /**
     * The is hidden.<br>
     */
    boolean isHidden = false;

    /**
     * The always show.<br>
     */
    boolean alwaysShow = false;

    /**
     * The points.<br>
     */
    int points = 1000;// research point
    /**
     * The is infinite.<br>
     */
    boolean infinite;

    /**
     * Instantiates a new Research.<br>
     */
    public Research() {
        this.id = Long.toHexString(UUID.randomUUID().getMostSignificantBits());
        this.icon = FHIcons.nop();
    }

    public Research(FHIcon icon, ResearchCategory category, List<Supplier<Research>> parents, List<Clue> clues, List<IngredientWithSize> requiredItems, List<Effect> effects, String name,
		List<String> desc, List<String> fdesc, boolean[] flags, int points) {
		super();
		this.icon = icon;
		this.category = category;
		if(parents!=null)
			this.parents.addAll(parents);
		this.clues.addAll(clues);
		this.requiredItems.addAll(requiredItems);
		this.effects.addAll(effects);
		this.name = name;
		this.desc.addAll(desc);
		this.fdesc.addAll(fdesc);
		this.showfdesc = flags[0];
		this.hideEffects = flags[1];
		this.inCompletable = flags[2];
		this.isHidden = flags[3];
		this.alwaysShow = flags[4];
		this.infinite = flags[5];
		this.points = points;
	}

    /**
     * Instantiates a new Research.<br>
     *
     * @param id       the id<br>
     * @param category the category<br>
     * @param icon     the icon<br>
     * @param parents  the parents<br>
     */
    @SafeVarargs
    public Research(String id, ResearchCategory category, ItemLike icon, Supplier<Research>... parents) {
        this(id, category, new ItemStack(icon), parents);
    }

    /**
     * Instantiates a new Research.<br>
     *
     * @param id       the id<br>
     * @param category the category<br>
     * @param icon     the icon<br>
     * @param parents  the parents<br>
     */
    @SafeVarargs
    public Research(String id, ResearchCategory category, ItemStack icon, Supplier<Research>... parents) {
        this.id = id;
        this.parents.addAll(Arrays.asList(parents));
        this.icon = FHIcons.getIcon(icon);
        this.setCategory(category);
    }

    /**
     * Instantiates a new Research.<br>
     *
     * @param path     the research id<br>
     * @param category the category<br>
     * @param parents  parents<br>
     */
    @SafeVarargs
    public Research(String path, ResearchCategory category, Supplier<Research>... parents) {
        this(path, category, new ItemStack(Items.AIR), parents);

    }

    /**
     * Adds the parent.
     *
     * @param par the par<br>
     */
    public void addParent(Supplier<Research> par) {
        this.parents.add(par);
    }

    /**
     * Attach clue.
     *
     * @param cl the cl<br>
     */
    public void attachClue(Clue cl) {
        clues.add(cl);
    }

    /**
     * Attach effect.
     *
     * @param effs the effs<br>
     */
    public void attachEffect(Effect... effs) {
        effects.addAll(Arrays.asList(effs));
    }

    /**
     * Attach required item.
     *
     * @param ingredients the ingredients<br>
     */
    public void attachRequiredItem(IngredientWithSize... ingredients) {
        requiredItems.addAll(Arrays.asList(ingredients));
    }

    /**
     * Delete.
     */
    public void delete() {
        deleteInTree();
        this.effects.forEach(Effect::deleteSelf);
        this.clues.forEach(Clue::deleteSelf);
        FHTeamDataManager.INSTANCE.getAllData().forEach(e -> e.getData(SpecialDataTypes.RESEARCH_DATA).resetData(this, false));

        FHResearch.delete(this);
    }

    private void deleteInTree() {
        this.getChildren().forEach(e -> e.removeParent(this));
        this.getParents().forEach(e -> e.children.removeIf(e2 -> e2.get() == this));
    }

    /**
     * Do index.
     */
    public void doIndex() {
        Supplier<Research> objthis = getSupplier();

        for (Supplier<Research> r : this.parents) {
            Research rx = r.get();
            if (rx != null)
                rx.populateChild(objthis);
        }
        int i = 0;
        effects.removeIf(Objects::isNull);
        clues.removeIf(Objects::isNull);
        for (Effect e : effects) {
            e.parent = getSupplier();
            FHResearch.effects.register(e);
            i++;
        }
        i = 0;
        for (Clue c : clues) {
            c.parent = getSupplier();
            FHResearch.clues.register(c);
            i++;
        }
    }

    /**
     * Do reindex.
     */
    public void doReindex() {
        children.clear();
    }

    /**
     * Get alt desc.
     *
     * @return alt desc<br>
     */
    public List<Component> getAltDesc() {
        return FHTextUtil.get(fdesc, "research", () -> id + ".desc_alt");
    }

    /**
     * Get category.
     *
     * @return category<br>
     */
    public ResearchCategory getCategory() {
        return category;
    }

    /**
     * Get children.
     *
     * @return children<br>
     */
    public Set<Research> getChildren() {
        return children.stream().map(Supplier::get).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /*
     * public int getTime() {
     * return time;
     * }
     */

    /**
     * Get clues.
     *
     * @return clues<br>
     */
    public List<Clue> getClues() {
        return clues;
    }

    /**
     * Get current points.
     *
     * @return current points<br>
     */
    @OnlyIn(Dist.CLIENT)
    public long getCurrentPoints() {
        return getData().getTotalCommitted();
    }

    /**
     * Get data.
     *
     * @return data<br>
     */
    @OnlyIn(Dist.CLIENT)
    public ResearchData getData() {
        ResearchData rd = ClientResearchDataAPI.getData().getData(this);
        if (rd == null)
            return ResearchData.EMPTY;
        return rd;
    }

    /**
     * Get data.
     *
     * @param team the team<br>
     * @return data<br>
     */
    public ResearchData getData(TeamDataHolder team) {
        return team.getData(SpecialDataTypes.RESEARCH_DATA).getData(this);
    }

    /**
     * Get desc.
     *
     * @return desc<br>
     */
    public List<Component> getDesc() {
        if (showfdesc && !isCompleted())
            return getAltDesc();
        return getODesc();
    }

    /**
     * Get effects.
     *
     * @return effects<br>
     */
    public List<Effect> getEffects() {
        return effects;
    }

    /**
     * Get icon.
     *
     * @return icon<br>
     */
    public FHIcon getIcon() {
        return icon;
    }

    /**
     * Get id.
     *
     * @return id<br>
     */
    public String getId() {
        return id;
    }


    /**
     * Get name.
     *
     * @return name<br>
     */
    public BaseComponent getName() {
        return (BaseComponent) FHTextUtil.get(name, "research", () -> id + ".name");
    }

    /**
     * Get o desc.
     *
     * @return o desc<br>
     */
    public List<Component> getODesc() {
        return FHTextUtil.get(desc, "research", () -> id + ".desc");
    }

    /**
     * Get parents.
     *
     * @return parents<br>
     */
    public Set<Research> getParents() {
        return parents.stream().filter(Objects::nonNull).map(Supplier::get).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * Get progress fraction.
     *
     * @return progress fraction<br>
     */
    @OnlyIn(Dist.CLIENT)
    public float getProgressFraction() {
        return getData().getProgress();
    }

    /**
     * Get required items.
     *
     * @return required items<br>
     */
    public List<IngredientWithSize> getRequiredItems() {
        return Collections.unmodifiableList(requiredItems);
    }

    /**
     * Get required points.
     *
     * @return required points<br>
     */
    public long getRequiredPoints() {
        return points;
    }

    /**
     * Get supplier.
     *
     * @return supplier<br>
     */
    public Supplier<Research> getSupplier() {
        return FHResearch.getResearch(this.getId());

    }

    /**
     * Grant effects.
     *
     * @param team the team<br>
     * @param spe  the spe<br>
     */
    public void grantEffects(TeamResearchData team, ServerPlayer spe) {
        boolean granted = true;
        for (Effect e : getEffects()) {
            team.grantEffect(e, spe);
            granted &= team.isEffectGranted(e);
        }
        if (infinite && granted) {
            int lvl = team.getData(this).getLevel();
            team.resetData(this, true);
            team.getData(this).setLevel(lvl + 1);
        }

    }

    /**
     * Checks for unclaimed reward.<br>
     *
     * @return true, if
     */
    @OnlyIn(Dist.CLIENT)
    public boolean hasUnclaimedReward() {
        if (!this.isCompleted()) return false;
        for (Effect e : this.getEffects())
            if (!e.isGranted()) return true;
        return false;
    }

    /**
     * Checks if is completed.<br>
     *
     * @return if is completed,true.
     */
    @OnlyIn(Dist.CLIENT)
    public boolean isCompleted() {
        return getData().isCompleted();
    }

    /**
     * Checks if is completed.<br>
     *
     * @param t the t<br>
     * @return if is completed,true.
     */
    public boolean isCompleted(TeamDataHolder t) {
        return getData(t).isCompleted();
    }

    /**
     * Checks if is hidden.<br>
     *
     * @return if is hidden,true.
     */
    public boolean isHidden() {
        return isHidden;
    }

    /**
     * Checks if is hide effects.<br>
     *
     * @return if is hide effects,true.
     */
    public boolean isHideEffects() {
        return hideEffects;
    }


    /**
     * Checks if is in completable.<br>
     *
     * @return if is in completable,true.
     */
    public boolean isInCompletable() {
        return inCompletable;
    }

    /**
     * Checks if is showable.<br>
     *
     * @return if is showable,true.
     */
    @OnlyIn(Dist.CLIENT)
    public boolean isShowable() {
        if (alwaysShow) return true;
        Set<Research> rs = this.getParents();
        if (rs.isEmpty()) return true;
        for (Research parent : rs) {
            if (parent.getData().isUnlocked()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if is unlocked.<br>
     *
     * @return if is unlocked,true.
     */
    @OnlyIn(Dist.CLIENT)
    public boolean isUnlocked() {
        for (Research parent : this.getParents()) {
            if (!parent.getData().isCompleted()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if is unlocked.<br>
     *
     * @param t the t<br>
     * @return if is unlocked,true.
     */
    public boolean isUnlocked(TeamDataHolder t) {
        for (Research parent : this.getParents()) {
            if (!parent.getData(t).isCompleted()) {
                return false;
            }
        }
        return true;
    }



    /**
     * Packet init, this would be call after everything is ready and packet is taking effect.
     */
    public void packetInit() {
        parents.clear();
    }

    /**
     * Populate child.
     *
     * @param child the child<br>
     */
    public void populateChild(Supplier<Research> child) {
        children.add(child);
    }

    public void reload() {
        for (Effect e : this.effects)
            e.reload();
    }

    /**
     * Removes the parent.
     *
     * @param parent the parent<br>
     */
    public void removeParent(Research parent) {
        this.parents.removeIf(e -> parent.equals(e.get()));
    }

    /**
     * Reset data.
     */
    @OnlyIn(Dist.CLIENT)
    public void resetData() {
    	ClientResearchDataAPI.getData().resetData(this, false);
    }

    /**
     * Send progress packet.
     *
     * @param team the team<br>
     */
    public void sendProgressPacket(TeamDataHolder team) {
        sendProgressPacket(team, getData(team));
    }

    /**
     * Send progress packet.
     *
     * @param team the team<br>
     * @param rd   the rd<br>
     */
    public void sendProgressPacket(TeamDataHolder team, ResearchData rd) {
        FHResearchDataUpdatePacket packet = new FHResearchDataUpdatePacket(rd);
        team.sendToOnline(packet);
    }
    /**
     * set category.
     *
     * @param category value to set category to.
     */
    public void setCategory(ResearchCategory category) {
        this.category = category;
    }

    /**
     * set id.
     *
     * @param id value to set id to.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * set in completable.
     *
     * @param inCompletable value to set in completable to.
     */
    public void setInCompletable(boolean inCompletable) {
        this.inCompletable = inCompletable;
    }

    /**
     * set new id.
     *
     * @param nid value to set new id to.
     */
    public void setNewId(String nid) {
        if (!id.equals(nid)) {
            FHTeamDataManager.INSTANCE.getAllData().forEach(e -> e.getData(SpecialDataTypes.RESEARCH_DATA).resetData(this, false));
            deleteInTree();//clear all reference, hope this could work
            FHResearch.delete(this);
            this.setId(nid);
            FHResearch.register(this);

            this.getChildren().forEach(e -> e.addParent(this.getSupplier()));
            this.getEffects().forEach(e -> FHResearch.effects.remove(e));
            this.getClues().forEach(e -> FHResearch.clues.remove(e));
            this.doIndex();
        }
    }

    /**
     * set parents.
     *
     * @param collect value to set parents to.
     */
    public void setParents(Collection<Supplier<Research>> collect) {
        this.parents.clear();
        this.parents.addAll(collect);
    }

    /**
     * set parents.
     *
     * @param parents value to set parents to.
     */
    @SafeVarargs
    public final void setParents(Supplier<Research>... parents) {
        this.parents.clear();
        this.parents.addAll(Arrays.asList(parents));
    }

    /**
     * To string.<br>
     *
     * @return returns to string
     */
    public String toString() {
        return "Research[" + id + "]";
    }

}
