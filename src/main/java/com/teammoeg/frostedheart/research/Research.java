package com.teammoeg.frostedheart.research;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.research.clues.Clue;
import com.teammoeg.frostedheart.research.clues.Clues;
import com.teammoeg.frostedheart.research.data.ResearchData;
import com.teammoeg.frostedheart.research.data.FHResearchDataManager;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.research.effects.Effects;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.research.network.FHResearchDataUpdatePacket;
import com.teammoeg.frostedheart.research.gui.FHTextUtil;
import com.teammoeg.frostedheart.util.SerializeUtil;
import com.teammoeg.frostedheart.util.Writeable;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Only Definition of research.
 * Part of Research Category {@link ResearchCategory}
 */
public class Research extends FHRegisteredItem implements Writeable {

    private String id;// id of this research
    FHIcon icon;// icon for this research in term of item
    private ResearchCategory category;
    private HashSet<Supplier<Research>> parents = new HashSet<>();// parent researches
    private HashSet<Supplier<Research>> children = new HashSet<>();// child researches, this is set automatically,
    // should not set manually.

    private List<Clue> clues = new ArrayList<>();// research clues
    List<IngredientWithSize> requiredItems = new ArrayList<>();
    private List<Effect> effects = new ArrayList<>();// effects of this research
    String name = "";
    List<String> desc;
    List<String> fdesc;
    boolean showfdesc;
    boolean hideEffects;
    private boolean inCompletable=false;
    boolean isHidden=false;
    boolean alwaysShow=false;
    long points = 1000;// research point

    @SafeVarargs
    public Research(String path, ResearchCategory category, Supplier<Research>... parents) {
        this(path, category, new ItemStack(Items.AIR), parents);

    }

    /*public ConfigGroup getConfigGroup() {
        ConfigGroup cg=new ConfigGroup("research");
        cg.addString("id", id,e->id=e,id);
        cg.addString("name", name,e->name=e,"");
        cg.addList("desc",desc,new StringConfig(null),"");

        cg.addItemStack("icon",icon instanceof FHItemIcon?((FHItemIcon) icon).getStack():ItemStack.EMPTY,i->{if(icon instanceof FHItemIcon||!i.isEmpty())icon=FHIcons.getIcon(i);},new ItemStack(FHItems.energy_core),true,true);
        cg.addEnum("category", category,c->category=c,NameMap.of(ResearchCategory.RESCUE,ResearchCategory.values()).icon(r->Icon.getIcon(r.getIcon())).id(r->r.getId().toString()).name(r->new StringTextComponent(r.name())).create());
        cg.addLong("points", points,l->this.points=l,2000, 0,Long.MAX_VALUE);

        return cg;
    }*/
    public Research() {
        this.id = Long.toHexString(UUID.randomUUID().getMostSignificantBits());
        this.icon = FHIcons.nop();
        desc = new ArrayList<>();
        fdesc = new ArrayList<>();
    }

    public Research(String id, JsonObject jo) {
        this.id = id;
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
        setCategory(ResearchCategories.ALL.get(new ResourceLocation(jo.get("category").getAsString())));
        if (jo.has("parents"))
            parents.addAll(
                    SerializeUtil.parseJsonElmList(jo.get("parents"), p -> FHResearch.researches.get(p.getAsString())));
        clues.addAll(SerializeUtil.parseJsonList(jo.get("clues"), Clues::read));
        requiredItems = SerializeUtil.parseJsonElmList(jo.get("ingredients"), IngredientWithSize::deserialize);
        effects = SerializeUtil.parseJsonList(jo.get("effects"), Effects::deserialize);
        points = jo.get("points").getAsInt();
        if (jo.has("showAltDesc"))
            showfdesc = jo.get("showAltDesc").getAsBoolean();
        if (jo.has("hideEffects"))
            hideEffects = jo.get("hideEffects").getAsBoolean();
        if(jo.has("hidden"))
        	isHidden=jo.get("hidden").getAsBoolean();
        if(jo.has("locked"))
        	inCompletable=jo.get("locked").getAsBoolean();
        if(jo.has("keepShow"))
        	alwaysShow=jo.get("keepShow").getAsBoolean();

    }

    @Override
    public JsonElement serialize() {
        JsonObject jo = new JsonObject();
        if (name.length() > 0 && !name.equals("@"))
            jo.addProperty("name", name);
        if (!desc.isEmpty())
            jo.add("desc", SerializeUtil.toJsonStringList(desc, e -> e));
        if (!fdesc.isEmpty())
            jo.add("descAlt", SerializeUtil.toJsonStringList(fdesc, e -> e));
        jo.add("icon", icon.serialize());
        jo.addProperty("category", category.getId().toString());
        if (!parents.isEmpty())
            jo.add("parents", SerializeUtil.toJsonStringList(parents, FHRegistry::serializeSupplier));
        jo.add("clues", SerializeUtil.toJsonList(clues, Clue::serialize));
        jo.add("ingredients", SerializeUtil.toJsonList(requiredItems, IngredientWithSize::serialize));
        jo.add("effects", SerializeUtil.toJsonList(effects, Writeable::serialize));
        jo.addProperty("points", points);
        if (showfdesc)
            jo.addProperty("showAltDesc", true);
        if (hideEffects)
            jo.addProperty("hideEffects", true);
        if(isHidden)
        	jo.addProperty("hidden", true);
        if(inCompletable)
        	jo.addProperty("locked", true);
        if(alwaysShow) {
        	jo.addProperty("keepShow",true);
        }
        return jo;
    }

    public Research(PacketBuffer data) {
        id = data.readString();
        //System.out.println("read "+id);
        name = data.readString();
        desc = SerializeUtil.readList(data, PacketBuffer::readString);
        fdesc = SerializeUtil.readList(data, PacketBuffer::readString);
        icon = FHIcons.readIcon(data);
        ResourceLocation rl = data.readResourceLocation();
        setCategory(ResearchCategories.ALL.get(rl));
        //System.out.println("category "+rl.toString());
        parents.addAll(SerializeUtil.readList(data, FHResearch.researches::readSupplier));
        clues.addAll(SerializeUtil.readList(data, Clues::read));
        requiredItems = SerializeUtil.readList(data, IngredientWithSize::read);
        effects = SerializeUtil.readList(data, Effects::deserialize);
        points = data.readVarLong();
        boolean[] bools=SerializeUtil.readBooleans(data);
        showfdesc = bools[0];
        hideEffects = bools[1];
        isHidden=bools[2];
        inCompletable=bools[3];
        alwaysShow=bools[4];
    }


    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeString(id);
        buffer.writeString(name);
        SerializeUtil.writeList2(buffer, desc, PacketBuffer::writeString);
        SerializeUtil.writeList2(buffer, fdesc, PacketBuffer::writeString);
        icon.write(buffer);
        buffer.writeResourceLocation(category.getId());
        SerializeUtil.writeList2(buffer, parents, FHRegistry::writeSupplier);
        SerializeUtil.writeList(buffer, clues, Clue::write);
        SerializeUtil.writeList(buffer, requiredItems, (e, p) -> e.write(p));
        SerializeUtil.writeList(buffer, effects, (e, p) -> e.write(p));
        buffer.writeVarLong(points);
        SerializeUtil.writeBooleans(buffer,showfdesc,hideEffects,isHidden,inCompletable,alwaysShow);
    }

    public List<Clue> getClues() {
        return clues;
    }

    public void attachClue(Clue cl) {
        clues.add(cl);
    }

    public List<Effect> getEffects() {
        return effects;
    }

    public void attachEffect(Effect... effs) {
        for (Effect effect : effs) {
            effects.add(effect);
        }
    }
    
    public void grantEffects(TeamResearchData team,ServerPlayerEntity spe) {
    	for (Effect e : getEffects())
            team.grantEffect(e, spe);
    }
    
    public List<IngredientWithSize> getRequiredItems() {
        return Collections.unmodifiableList(requiredItems);
    }

    public void attachRequiredItem(IngredientWithSize... ingredients) {
        for (IngredientWithSize ingredient : ingredients) {
            requiredItems.add(ingredient);
        }
    }

    @SafeVarargs
    public Research(String id, ResearchCategory category, IItemProvider icon, Supplier<Research>... parents) {
        this(id, category, new ItemStack(icon), parents);
    }

    @SafeVarargs
    public Research(String id, ResearchCategory category, ItemStack icon, Supplier<Research>... parents) {
        this.id = id;
        this.parents.addAll(Arrays.asList(parents));
        this.icon = FHIcons.getIcon(icon);
        this.setCategory(category);
        desc = new ArrayList<>();
    }

    /*
     * public int getTime() {
     * return time;
     * }
     */

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Supplier<Research> getSupplier() {
        return FHResearch.getResearch(this.getLId());

    }

    public void doIndex() {
        Supplier<Research> objthis = getSupplier();
        for (Supplier<Research> r : this.parents) {
            Research rx = r.get();
            if (rx != null)
                rx.populateChild(objthis);
        }
        int i = 0;
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

    public void populateChild(Supplier<Research> child) {
        children.add(child);
    }

    public Set<Research> getChildren() {
        return children.stream().map(r -> r.get()).filter(e -> e != null).collect(Collectors.toSet());
    }

    public Set<Research> getParents() {
        return parents.stream().filter(e -> e != null).map(r -> r.get()).filter(e -> e != null).collect(Collectors.toSet());
    }

    @SafeVarargs
    public final void setParents(Supplier<Research>... parents) {
        this.parents.clear();
        this.parents.addAll(Arrays.asList(parents));
    }

    public FHIcon getIcon() {
        return icon;
    }

    public TextComponent getName() {
        return (TextComponent) FHTextUtil.get(name, "research", () -> id + ".name");
    }

    public List<ITextComponent> getDesc() {
        if (showfdesc && !isCompleted())
            return getAltDesc();
        return getODesc();
    }

    public List<ITextComponent> getODesc() {
        return FHTextUtil.get(desc, "research", () -> id + ".desc");
    }

    public List<ITextComponent> getAltDesc() {
        return FHTextUtil.get(fdesc, "research", () -> id + ".desc_alt");
    }

    public ResearchCategory getCategory() {
        return category;
    }

    public long getRequiredPoints() {
        return points;
    }

    @OnlyIn(Dist.CLIENT)
    public long getCurrentPoints() {
        return getData().getTotalCommitted();
    }

    @OnlyIn(Dist.CLIENT)
    public float getProgressFraction() {
        return getData().getProgress();
    }

    public ResearchData getData(Team team) {
        return FHResearchDataManager.INSTANCE.getData(team.getId()).getData(this);
    }

    @OnlyIn(Dist.CLIENT)
    public ResearchData getData() {
        return TeamResearchData.getClientInstance().getData(this);
    }

    @OnlyIn(Dist.CLIENT)
    public void resetData() {
        TeamResearchData.getClientInstance().resetData(this);
    }

    public void sendProgressPacket(Team team) {
        sendProgressPacket(team, getData(team));
    }

    public void sendProgressPacket(Team team, ResearchData rd) {
        FHResearchDataUpdatePacket packet = new FHResearchDataUpdatePacket(rd);
        for (ServerPlayerEntity spe : team.getOnlineMembers())
            PacketHandler.send(PacketDistributor.PLAYER.with(() -> spe), packet);
    }

    public String toString() {
        return "Research[" + id + "]";
    }

    @Override
    public String getLId() {
        return id;
    }

    public boolean isCompleted(Team t) {
        return getData(t).isCompleted();
    }

    public boolean isUnlocked(Team t) {
        for (Research parent : this.getParents()) {
            if (!parent.getData(t).isCompleted()) {
                return false;
            }
        }
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isCompleted() {
        return getData().isCompleted();
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isUnlocked() {
        for (Research parent : this.getParents()) {
            if (!parent.getData().isCompleted()) {
                return false;
            }
        }
        return true;
    }
    @OnlyIn(Dist.CLIENT)
    public boolean isShowable() {
    	if(alwaysShow)return true;
    	Set<Research> rs=this.getParents();
    	if(rs.isEmpty())return true;
        for (Research parent : rs) {
            if (parent.getData().isUnlocked()) {
                return true;
            }
        }
        return false;
    }
    
    public void setCategory(ResearchCategory category) {
        this.category = category;
    }

    private void deleteInTree() {
        this.getChildren().forEach(e -> e.removeParent(this));
        this.getParents().forEach(e -> e.children.removeIf(e2 -> e2.get() == this));
    }

    public void delete() {
        deleteInTree();
        this.effects.forEach(Effect::deleteSelf);
        this.clues.forEach(Clue::deleteSelf);
        FHResearchDataManager.INSTANCE.getAllData().forEach(e -> e.resetData(this));

        FHResearch.delete(this);
    }

    public void setParents(Collection<Supplier<Research>> collect) {
        this.parents.clear();
        this.parents.addAll(collect);
    }

    public void removeParent(Research parent) {
        this.parents.removeIf(e -> parent == e.get());
    }

    public void addParent(Supplier<Research> par) {
        this.parents.add(par);
    }

    public void setNewId(String nid) {
        if (!id.equals(nid)) {
            FHResearchDataManager.INSTANCE.getAllData().forEach(e -> e.resetData(this));
            deleteInTree();//clear all reference, hope this could work
            FHResearch.delete(this);
            this.setId(nid);
            FHResearch.register(this);
            this.getChildren().forEach(e -> e.addParent(this.getSupplier()));
            this.doIndex();
        }
    }

    public boolean isHideEffects() {
        return hideEffects;
    }

	public boolean isInCompletable() {
		return inCompletable;
	}

	public void setInCompletable(boolean inCompletable) {
		this.inCompletable = inCompletable;
	}

}
