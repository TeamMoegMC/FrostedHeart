package com.teammoeg.frostedheart.research;

import com.teammoeg.frostedheart.network.FHResearchProgressSyncPacket;
import com.teammoeg.frostedheart.network.PacketHandler;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
    private ArrayList<ItemStack> requireItems=new ArrayList<>();
    private int points;
    private int requiredTicks;
    private int finishedTicks;
    // the time required to complete research after starting research
    // with required items are consumed and clues are found and other requirements satisfied

    @SafeVarargs
	public Research(String path, ResearchCategory category, Supplier<Research>... parents) {
        this(path, category, Items.GRASS_BLOCK, parents);
    }


    @SafeVarargs
	public Research(String id, ResearchCategory category, Item icon, Supplier<Research>... parents) {
        this.id = id;
        this.parents.addAll(Arrays.asList(parents));
        this.name = new TranslationTextComponent("research."+id+ ".name");
        this.desc = new TranslationTextComponent("research."+id + ".desc");
        this.icon = icon;
        this.category = category;
        this.requiredTicks = 1200;
        this.finishedTicks = 600;
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
        return parents.stream().map(r->r.get()).collect(Collectors.toSet());
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

    public int getRequiredTicks() {
        return requiredTicks;
    }

    public int getFinishedTicks() {
        return finishedTicks;
    }

    public float getProgressFraction() {
        return (float) finishedTicks / requiredTicks;
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
