package com.teammoeg.frostedheart.research;

import com.teammoeg.frostedheart.network.FHResearchProgressSyncPacket;
import com.teammoeg.frostedheart.network.PacketHandler;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Only Definition of research.
 * Part of Research Category {@link ResearchCategory}
 *
 */
public class Research extends FHRegisteredItem{
    private String id;
    private TranslationTextComponent name;
    private TranslationTextComponent desc;
    private Item icon;
    private HashSet<Supplier<Research>> parents = new HashSet<>();
    private HashSet<Supplier<Research>> children = new HashSet<>();
    private HashSet<Supplier<AbstractClue>> clues=new HashSet<>();
    private ResearchCategory category;
    public Set<AbstractClue> getClues() {
		return clues.stream().map(e->e.get()).collect(Collectors.toSet());
	}
    public void attachClue(Supplier<AbstractClue> cl) {
		clues.add(cl);
	}
	private ArrayList<IngredientWithSize> requiredItems=new ArrayList<>();

	private int points;//research point
    private int time;//time cost per research point commit.

    @SafeVarargs
	public Research(String path, ResearchCategory category, Supplier<Research>... parents) {
        this(path, category, Items.GRASS_BLOCK, parents);
    }

    public List<IngredientWithSize> getRequiredItems() {
		return Collections.unmodifiableList(requiredItems);
	}
    @SafeVarargs
	public Research(String id, ResearchCategory category, Item icon, Supplier<Research>... parents) {
        this.id = id;
        this.parents.addAll(Arrays.asList(parents));
        this.name = new TranslationTextComponent("research."+id+ ".name");
        this.desc = new TranslationTextComponent("research."+id + ".desc");
        this.icon = icon;
        this.category = category;
        this.time = 20;
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    public Supplier<Research> getSupplier(){
		return FHResearch.getResearch(this.getLId());
    	
    }
    public void doIndex() {
    	Supplier<Research> objthis=getSupplier();
    	for(Supplier<Research> r:this.parents) {
    		r.get().populateChild(objthis);
    	}
    }
    public void populateChild(Supplier<Research> child) {
    	children.add(child);
    }
    public Set<Research> getChildren() {
        return children.stream().map(r->r.get()).collect(Collectors.toSet());
    }
    public Set<Research> getParents() {
        return parents.stream().map(r->r.get()).collect(Collectors.toSet());
    }

    public void setParents(Supplier<Research>... parents) {
        this.parents.clear();
        this.parents.addAll(Arrays.asList(parents));
    }
    
    public Item getIcon() {
        return icon;
    }

    public TranslationTextComponent getName() {
        return name;
    }

    public TranslationTextComponent getDesc() {
        return desc;
    }

    public ResearchCategory getCategory() {
        return category;
    }

    public int getRequiredPoints() {
        return points;
    }
    @OnlyIn(Dist.CLIENT)
    public int getCurrentPoints() {
        return getData().getTotalCommitted();
    }
    @OnlyIn(Dist.CLIENT)
    public float getProgressFraction() {
        return getData().getProgress();
    }

    public ResearchData getData(Team team) {
    	return ResearchDataManager.INSTANCE.getData(team.getId()).getData(this);
    }
    @OnlyIn(Dist.CLIENT)
    public ResearchData getData() {
    	return TeamResearchData.INSTANCE.getData(this);
    }
    public void sendProgressPacket(Team team) {
    	FHResearchProgressSyncPacket packet=new FHResearchProgressSyncPacket(team.getId(),this);
    	for(ServerPlayerEntity spe:team.getOnlineMembers())
    		PacketHandler.send(PacketDistributor.PLAYER.with(()->spe),packet);
    }

    public String toString() {
        return "Research[" + id + "]";
    }

	@Override
	public String getLId() {
		return id.toString();
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
}
