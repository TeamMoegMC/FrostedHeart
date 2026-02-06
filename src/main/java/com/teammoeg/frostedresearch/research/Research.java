/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedresearch.research;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.util.struct.OptionalLazy;
import com.teammoeg.frostedresearch.FHRegisteredItem;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.data.ResearchData;
import com.teammoeg.frostedresearch.gui.FRTextUtil;
import com.teammoeg.frostedresearch.number.IResearchNumber;
import com.teammoeg.frostedresearch.research.clues.Clue;
import com.teammoeg.frostedresearch.research.effects.Effect;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Only Definition of research.
 *
 * @author khjxiaogu
 */

public class Research implements FHRegisteredItem {
    public static final Codec<Research> CODEC = RecordCodecBuilder.create(t -> t.group(
            CIcons.CODEC.optionalFieldOf("icon", CIcons.nop()).forGetter(o -> o.icon),
            ResearchCategory.CODEC.fieldOf("category").forGetter(o -> o.category),
            Codec.list(Codec.STRING).optionalFieldOf("parents", Arrays.asList()).forGetter(o -> new ArrayList<>(o.parents)),
            Codec.list(Clue.CODEC).optionalFieldOf("clues", Arrays.asList()).forGetter(o -> o.clues),
            Codec.list(CodecUtil.INGREDIENT_SIZE_CODEC).optionalFieldOf("ingredients", Arrays.asList()).forGetter(o -> o.requiredItems),
            Codec.list(Effect.CODEC).optionalFieldOf("effects").forGetter(o -> o.effects.size() > 0 ? Optional.of(o.effects) : Optional.empty()),
            Codec.STRING.optionalFieldOf("name", "").forGetter(o -> o.name),
            Codec.list(Codec.STRING).optionalFieldOf("desc", Arrays.asList()).forGetter(o -> o.desc),
            Codec.list(Codec.STRING).optionalFieldOf("descAlt", Arrays.asList()).forGetter(o -> o.fdesc),
            CodecUtil.<Research>booleans("flags")
                    .flag("showAltDesc", o -> o.showfdesc)
                    .flag("hideEffects", o -> o.hideEffects)
                    .flag("locked", o -> o.inCompletable)
                    .flag("hidden", o -> o.isHidden)
                    .flag("keepShow", o -> o.alwaysShow)
                    .flag("infinite", o -> o.isInfinite()).build(),
            Codec.INT.fieldOf("points").forGetter(o -> o.points),
            Codec.INT.optionalFieldOf("insight",1).forGetter(o->o.getInsight())
    ).apply(t, Research::new));
    /**
     * The icon for this research.<br>
     */
    CIcon icon;
    /**
     * The required items.<br>
     */
    List<Pair<Ingredient,Integer>> requiredItems = new ArrayList<>();
    List<IResearchNumber> requiredItemsCountOverride = new ArrayList<>();
    /**
     * The name.<br>
     */
    String name = "";
    /**
     * The desc.<br>
     */
    List<String> desc = new ArrayList<>();
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
    int insight = 1;//insight point
    private String id;// id of this research
    private ResearchCategory category = ResearchCategory.RESCUE;
    HashSet<String> parents = new HashSet<>();// parent researches
    HashSet<String> children = new HashSet<>();// child researches, this is set automatically,
    // should not set manually.
    private List<Clue> clues = new ArrayList<>();// research clues
    private List<Effect> effects = new ArrayList<>();// effects of this research
    private boolean inCompletable = false;
    /**
     * The is infinite.<br>
     */
    private boolean infinite;

    /**
     * Instantiates a new Research.<br>
     */
    public Research() {
        this.id = Long.toHexString(UUID.randomUUID().getMostSignificantBits());
        this.icon = CIcons.nop();
    }

    public Research(String id,String name,int insight, int points, CIcon icon,ResearchCategory category,Collection<String> desc, Collection<String> fdesc,Collection<Research> parents,Collection<Research> children, List<Pair<Ingredient, Integer>> requiredItems, Collection<Effect> effects, Collection<Clue> clues,  
    	boolean alwaysShow, boolean hideEffects, boolean showfdesc,  
		boolean infinite, boolean isHidden,boolean inCompletable) {
		super();
		this.icon = icon;
		this.requiredItems = requiredItems;
		this.name = name;
		this.desc.addAll(desc);
		this.fdesc.addAll(fdesc);
		this.showfdesc = showfdesc;
		this.hideEffects = hideEffects;
		this.isHidden = isHidden;
		this.alwaysShow = alwaysShow;
		this.points = points;
		this.insight = insight;
		parents.forEach(r->this.parents.add(r.getId()));
		children.forEach(r->this.children.add(r.getId()));
		this.id = id;
		this.category = category;
		this.clues.addAll(clues);
		this.effects.addAll(effects);
		this.inCompletable = inCompletable;
		this.infinite = infinite;
	}

	public Research(CIcon icon, ResearchCategory category, List<String> parents, List<Clue> clues, List<Pair<Ingredient,Integer>> requiredItems, Optional<List<Effect>> effects, String name,
                    List<String> desc, List<String> fdesc, boolean[] flags, int points,int insight) {
        super();
        this.icon = icon;
        this.category = category;
        if (parents != null)
            this.parents.addAll(parents);
        //System.out.println(parents);
        this.clues.addAll(clues);

        this.requiredItems.addAll(requiredItems);
        effects.ifPresent(this.effects::addAll);

        this.name = name;
        this.desc.addAll(desc);
        this.fdesc.addAll(fdesc);
        this.showfdesc = flags[0];
        this.hideEffects = flags[1];
        this.inCompletable = flags[2];
        this.isHidden = flags[3];
        this.alwaysShow = flags[4];
        this.setInfinite(flags[5]);
        this.points = points;
        this.insight=insight;
//		System.out.println(effects);
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
    public Research(String id, ResearchCategory category, ItemLike icon, String... parents) {
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
    public Research(String id, ResearchCategory category, ItemStack icon, String... parents) {
        this.id = id;
        this.parents.addAll(Arrays.asList(parents));
        this.icon = CIcons.getIcon(icon);
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
    public Research(String path, ResearchCategory category, String... parents) {
        this(path, category, new ItemStack(Items.AIR), parents);

    }

    /**
     * Adds the parent.
     *
     * @param par the par<br>
     */
    public void addParent(String par) {
        this.parents.add(par);
    }
    public boolean hasParent(String par) {
    	return this.parents.contains(par);
    }
    public boolean hasParent(Research rs) {
    	return hasParent(rs.getId());
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
    public void attachRequiredItem(@SuppressWarnings("unchecked") Pair<Ingredient,Integer>... ingredients) {
        requiredItems.addAll(Arrays.asList(ingredients));
    }

    /**
     * Delete.
     */
    public void delete() {
        deleteInTree();
        //this.effects.forEach(Effect::deleteSelf);
        //this.clues.forEach(Clue::deleteSelf);
        CTeamDataManager.INSTANCE.getAllData().forEach(e -> e.getData(FRSpecialDataTypes.RESEARCH_DATA).resetData(e, this));

        FHResearch.delete(this);
    }

    private void deleteInTree() {
        this.getChildren().forEach(e -> e.removeParent(this));
        this.getParents().forEach(e -> e.children.removeIf(e2 -> e2.equals(this.getId())));
    }

    /**
     * Do index.
     */
    public void doIndex() {

        for (String r : this.parents) {
            Research rx = FHResearch.getResearch(r);
            if (rx != null)
                rx.populateChild(this);
        }
        int i = 0;
        effects.removeIf(Objects::isNull);
        effects.forEach(t -> t.init());
        clues.removeIf(Objects::isNull);
        clues.forEach(t -> t.init(this));
       /* for (Effect e : effects) {
            e.parent = this;
            FHResearch.effects.register(e);
            i++;
        }
        i = 0;
        for (Clue c : clues) {
            c.parent = this;
            FHResearch.clues.register(c);
            i++;
        }*/
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
        return FRTextUtil.get(fdesc, "research", id + ".desc_alt");
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
     * set category.
     *
     * @param category value to set category to.
     */
    public void setCategory(ResearchCategory category) {
        this.category = category;
    }

    /*
     * public int getTime() {
     * return time;
     * }
     */

    /**
     * Get children.
     *
     * @return children<br>
     */
    public Collection<Research> getChildren() {
        return children.stream().map(FHResearch::getResearch).filter(Objects::nonNull).collect(Collectors.toSet());
    }

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
        return getData().getTotalCommitted(this);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isInProgress() {
        Supplier<Research> r = ClientResearchDataAPI.getData().get().getCurrentResearch();
        Research rs=r.get();
        if (rs!=null) {
            return rs.equals(this);
        }
        return false;
    }

    /**
     * Get data.
     *
     * @return data<br>
     */
    @OnlyIn(Dist.CLIENT)
    public ResearchData getData() {
        ResearchData rd = ClientResearchDataAPI.getData().get().getData(this);
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
        return team.getData(FRSpecialDataTypes.RESEARCH_DATA).getData(this);
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
    public CIcon getIcon() {
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
     * set id.
     *
     * @param id value to set id to.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get name.
     *
     * @return name<br>
     */
    public Component getName() {
        return FRTextUtil.get(name, "research", id + ".name");
    }

    /**
     * Get o desc.
     *
     * @return o desc<br>
     */
    public List<Component> getODesc() {
        return FRTextUtil.get(desc, "research",  id + ".desc");
    }

    /**
     * Get parents.
     *
     * @return parents<br>
     */
    public Set<Research> getParents() {
        return parents.stream().filter(Objects::nonNull).map(FHResearch::getResearch).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * set parents.
     *
     * @param collect value to set parents to.
     */
    public void setParents(Collection<String> collect) {
        this.parents.clear();
        this.parents.addAll(collect);
    }

    /**
     * set parents.
     *
     * @param parents value to set parents to.
     */
    @SafeVarargs
    public final void setParents(String... parents) {
        this.parents.clear();
        this.parents.addAll(Arrays.asList(parents));
    }

    public Set<String> getParentIds() {
        return parents;
    }

    /**
     * Get progress fraction.
     *
     * @return progress fraction<br>
     */
    @OnlyIn(Dist.CLIENT)
    public float getProgressFraction() {
        return getData().getProgress(this);
    }

    /**
     * Get required items.
     *
     * @return required items<br>
     */
    public List<Pair<Ingredient,Integer>> getRequiredItems() {
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
     * Checks for unclaimed reward.<br>
     *
     * @return true, if
     */
    @OnlyIn(Dist.CLIENT)
    public boolean hasUnclaimedReward() {
        ResearchData rd = getData();
        if (!this.isCompleted()) return false;
        for (Effect e : this.getEffects())
            if (!rd.isEffectGranted(e)) return true;
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
     * set in completable.
     *
     * @param inCompletable value to set in completable to.
     */
    public void setInCompletable(boolean inCompletable) {
        this.inCompletable = inCompletable;
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
            if (parent.isUnlocked()) {
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
        //parents.clear();
    }

    /**
     * Populate child.
     *
     * @param child the child<br>
     */
    public void populateChild(Research child) {
        children.add(child.getId());
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
        this.parents.removeIf(e -> parent.getId().equals(e));
    }

    /**
     * Reset data.
     */
    @OnlyIn(Dist.CLIENT)
    public void resetData() {
        ClientResearchDataAPI.getData().get().resetData(null, this);
    }

    /**
     * set new id.
     *
     * @param nid value to set new id to.
     */
    public void setNewId(String nid) {
        if (!id.equals(nid)) {
            CTeamDataManager.INSTANCE.getAllData().forEach(e -> e.getData(FRSpecialDataTypes.RESEARCH_DATA).resetData(e, this));
            deleteInTree();//clear all reference, hope this could work
            FHResearch.delete(this);
            this.setId(nid);
            FHResearch.register(this);

            this.getChildren().forEach(e -> e.addParent(this.getId()));
            //this.getEffects().forEach(e -> FHResearch.effects.remove(e));
            //this.getClues().forEach(e -> FHResearch.clues.remove(e));
            this.doIndex();
        }
    }

    /**
     * To string.<br>
     *
     * @return returns to string
     */
    public String toString() {
        return "Research[" + id + "]";
    }

    public void addParent(Research r) {
        addParent(r.getId());

    }

    public boolean isInfinite() {
        return infinite;
    }

    public void setInfinite(boolean infinite) {
        this.infinite = infinite;
    }

	public int getInsight() {
		return insight;
	}

	@Override
	public int hashCode() {
		return Objects.hash(alwaysShow, category, children, clues, desc, effects, fdesc, hideEffects, icon, id, inCompletable, infinite, insight, isHidden, name, parents, points, requiredItems,
			requiredItemsCountOverride, showfdesc);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Research other = (Research) obj;
		return alwaysShow == other.alwaysShow && category == other.category && Objects.equals(children, other.children) && Objects.equals(clues, other.clues) && Objects.equals(desc, other.desc)
			&& Objects.equals(effects, other.effects) && Objects.equals(fdesc, other.fdesc) && hideEffects == other.hideEffects && Objects.equals(icon, other.icon) && Objects.equals(id, other.id)
			&& inCompletable == other.inCompletable && infinite == other.infinite && insight == other.insight && isHidden == other.isHidden && Objects.equals(name, other.name)
			&& Objects.equals(parents, other.parents) && points == other.points && Objects.equals(requiredItems, other.requiredItems)
			&& Objects.equals(requiredItemsCountOverride, other.requiredItemsCountOverride) && showfdesc == other.showfdesc;
	}

}
