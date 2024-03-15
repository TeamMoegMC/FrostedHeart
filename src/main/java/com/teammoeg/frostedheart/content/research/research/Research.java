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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.FHTeamDataManager;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.research.FHRegisteredItem;
import com.teammoeg.frostedheart.content.research.FHRegistry;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.SpecialResearch;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.data.ResearchData;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.gui.FHIcons;
import com.teammoeg.frostedheart.content.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.content.research.gui.FHTextUtil;
import com.teammoeg.frostedheart.content.research.network.FHResearchDataUpdatePacket;
import com.teammoeg.frostedheart.content.research.number.IResearchNumber;
import com.teammoeg.frostedheart.content.research.research.clues.Clue;
import com.teammoeg.frostedheart.content.research.research.clues.Clues;
import com.teammoeg.frostedheart.content.research.research.effects.Effect;
import com.teammoeg.frostedheart.content.research.research.effects.Effects;
import com.teammoeg.frostedheart.util.io.SerializeUtil;
import com.teammoeg.frostedheart.util.io.Writeable;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Only Definition of research.
 *
 * @author khjxiaogu
 */
public class Research extends FHRegisteredItem implements Writeable {

    private String id;// id of this research

    /**
     * The icon for this research.<br>
     */
    FHIcon icon;
    private ResearchCategory category;
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
    List<String> desc;

    /**
     * The fdesc.<br>
     */
    List<String> fdesc;

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
    long points = 1000;// research point

    /**
     * The is infinite.<br>
     */
    boolean infinite;

    private ResourceLocation categoryRL;

    private List<Integer> parentIds;

    /**
     * Instantiates a new Research.<br>
     */
    public Research() {
        this.id = Long.toHexString(UUID.randomUUID().getMostSignificantBits());
        this.icon = FHIcons.nop();
        desc = new ArrayList<>();
        fdesc = new ArrayList<>();
    }
    /*public static final Codec<Research> CODEC=RecordCodecBuilder.create(t->t.group(
    	Codec.STRING.fieldOf("id").forGetter(o->o.id),
    	Codec.STRING.fieldOf("name").forGetter(o->o.name),
    	Codec.list(Codec.STRING).fieldOf("desc").forGetter(o->o.desc),
    	Codec.list(Codec.STRING).fieldOf("fdesc").forGetter(o->o.fdesc)
    	));*/
    /**
     * Instantiates a new Research with a PacketBuffer object.<br>
     * This would be called before research registry and category init.
     * Shouldn't call methods provide by these in this method.
     *
     * @param data the packet<br>
     */
    public Research(PacketBuffer data) {
        id = data.readString();
        //System.out.println("read "+id);
        name = data.readString();
        desc = SerializeUtil.readList(data, PacketBuffer::readString);
        fdesc = SerializeUtil.readList(data, PacketBuffer::readString);
        icon = FHIcons.readIcon(data);
        categoryRL = data.readResourceLocation();
        parentIds = SerializeUtil.readList(data, PacketBuffer::readVarInt);
        //System.out.println("category "+rl.toString());

        clues.addAll(SerializeUtil.readList(data, Clues::read));
        requiredItems = SerializeUtil.readList(data, IngredientWithSize::read);
        effects = SerializeUtil.readList(data, Effects::read);
        points = data.readVarLong();
        boolean[] bools = SerializeUtil.readBooleans(data);
        showfdesc = bools[0];
        hideEffects = bools[1];
        isHidden = bools[2];
        inCompletable = bools[3];
        alwaysShow = bools[4];
        infinite = bools[5];
    }

    /**
     * Instantiates a new Research read from json.<br>
     *
     * @param id the id<br>
     * @param jo the json<br>
     */
    public Research(String id, JsonObject jo) {
        this.id = id;
        load(jo);

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
    public Research(String id, ResearchCategory category, IItemProvider icon, Supplier<Research>... parents) {
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
        desc = new ArrayList<>();
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
            e.addID(this.getLId(), i);
            e.parent = getSupplier();
            FHResearch.effects.register(e);
            i++;
        }
        i = 0;
        for (Clue c : clues) {
            c.addID(this.getLId(), i);
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
    public List<ITextComponent> getAltDesc() {
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
    public List<ITextComponent> getDesc() {
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
     * Get l id.
     *
     * @return l id<br>
     */
    @Override
    public String getLId() {
        return id;
    }

    /**
     * Get name.
     *
     * @return name<br>
     */
    public TextComponent getName() {
        return (TextComponent) FHTextUtil.get(name, "research", () -> id + ".name");
    }

    /**
     * Get o desc.
     *
     * @return o desc<br>
     */
    public List<ITextComponent> getODesc() {
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
        return FHResearch.getResearch(this.getLId());

    }

    /**
     * Grant effects.
     *
     * @param team the team<br>
     * @param spe  the spe<br>
     */
    public void grantEffects(TeamResearchData team, ServerPlayerEntity spe) {
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
     * Load from json
     *
     * @param jo the jo<br>
     */
    public void load(JsonObject jo) {
        if (jo.has("name"))
            name = jo.get("name").getAsString();
        if (jo.has("desc"))
            desc = SerializeUtil.parseJsonElmList(jo.get("desc"), JsonElement::getAsString);
        else
            desc = new ArrayList<>();
        if (jo.has("descAlt"))
            fdesc = SerializeUtil.parseJsonElmList(jo.get("descAlt"), JsonElement::getAsString);
        else
            fdesc = new ArrayList<>();
        icon = FHIcons.getIcon(jo.get("icon"));
        setCategory(ResearchCategory.ALL.get(new ResourceLocation(jo.get("category").getAsString())));

        if (jo.has("parents"))
            parents.addAll(
                    SerializeUtil.parseJsonElmList(jo.get("parents"), p -> FHResearch.researches.get(p.getAsString())));
        else
            parents.clear();
        clues = SerializeUtil.parseJsonList(jo.get("clues"), Clues::read);
        requiredItems = SerializeUtil.parseJsonElmList(jo.get("ingredients"), IngredientWithSize::deserialize);
        effects = SerializeUtil.parseJsonList(jo.get("effects"), Effects::deserialize);
        points = jo.get("points").getAsInt();
        if (jo.has("showAltDesc"))
            showfdesc = jo.get("showAltDesc").getAsBoolean();
        else
            showfdesc = false;
        if (jo.has("hideEffects"))
            hideEffects = jo.get("hideEffects").getAsBoolean();
        else
            hideEffects = false;
        if (jo.has("hidden"))
            isHidden = jo.get("hidden").getAsBoolean();
        else
            isHidden = false;
        if (jo.has("locked"))
            inCompletable = jo.get("locked").getAsBoolean();
        else
            inCompletable = false;
        if (jo.has("keepShow"))
            alwaysShow = jo.get("keepShow").getAsBoolean();
        else
            alwaysShow = false;
        if (jo.has("infinite"))
            infinite = jo.get("infinite").getAsBoolean();
        else
            infinite = false;
    }

    /**
     * Packet init, this would be call after everything is ready and packet is taking effect.
     */
    public void packetInit() {
        setCategory(ResearchCategory.ALL.get(categoryRL));
        parents.clear();
        parentIds.stream().map(FHResearch.researches::get).forEach(parents::add);
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
     * Serialize to json.<br>
     *
     * @return returns serialize
     */
    @Override
    public JsonElement serialize() {
        JsonObject jo = new JsonObject();
        if (!name.isEmpty())
            jo.addProperty("name", name);
        if (!desc.isEmpty())
            jo.add("desc", SerializeUtil.toJsonStringList(desc, e -> e));
        if (!fdesc.isEmpty())
            jo.add("descAlt", SerializeUtil.toJsonStringList(fdesc, e -> e));
        jo.add("icon", FHIcons.save(icon));
        jo.addProperty("category", category.getId().toString());
        if (!parents.isEmpty())
            jo.add("parents", SerializeUtil.toJsonStringList(parents, FHRegistry::serializeSupplier));
        jo.add("clues", SerializeUtil.toJsonList(clues, Clues::write));
        jo.add("ingredients", SerializeUtil.toJsonList(requiredItems, IngredientWithSize::serialize));
        jo.add("effects", SerializeUtil.toJsonList(effects, Effects::write));
        jo.addProperty("points", points);
        if (showfdesc)
            jo.addProperty("showAltDesc", true);
        if (hideEffects)
            jo.addProperty("hideEffects", true);
        if (isHidden)
            jo.addProperty("hidden", true);
        if (inCompletable)
            jo.addProperty("locked", true);
        if (alwaysShow)
            jo.addProperty("keepShow", true);
        if (infinite)
            jo.addProperty("infinite", true);
        return jo;
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
            this.getEffects().forEach(e -> e.setRId(0));
            this.getClues().forEach(e -> e.setRId(0));
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

    /**
     * Write to packet.
     *
     * @param buffer the packet<br>
     */
    @Override
    public void write(PacketBuffer buffer) {
        SpecialResearch.writeId(this, buffer);
        buffer.writeString(id);
        buffer.writeString(name);
        SerializeUtil.writeList2(buffer, desc, PacketBuffer::writeString);
        SerializeUtil.writeList2(buffer, fdesc, PacketBuffer::writeString);
        FHIcons.write(icon, buffer);
        buffer.writeResourceLocation(category.getId());
        SerializeUtil.writeList2(buffer, parents, FHRegistry::writeSupplier);
        SerializeUtil.writeList2(buffer, clues, Clues::write);
        SerializeUtil.writeList(buffer, requiredItems, IngredientWithSize::write);
        SerializeUtil.writeList(buffer, effects, Effects::write);
        buffer.writeVarLong(points);
        SerializeUtil.writeBooleans(buffer, showfdesc, hideEffects, isHidden, inCompletable, alwaysShow, infinite);
    }
}
