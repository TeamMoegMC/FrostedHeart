package com.teammoeg.frostedheart.research;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.network.FHResearchProgressSyncPacket;
import com.teammoeg.frostedheart.network.PacketHandler;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * Only Definition of research.
 * Part of Research Category {@link ResearchCategory}
 *
 */
public class Research extends FHRegisteredItem{
    private ResourceLocation id;
    private TranslationTextComponent name;
    private TranslationTextComponent desc;
    private Item icon;
    private HashSet<Research> parents = new HashSet<>();
    private HashSet<AbstractClue> clues=new HashSet<>();
    private ResearchCategory category;
    private ArrayList<ItemStack> requireItems=new ArrayList<>();
    private int points;

    public Research(String path, ResearchCategory category, Research... parents) {
        this(new ResourceLocation(FHMain.MODID, path), category, Items.GRASS_BLOCK, parents);
    }

    public Research(String path, ResearchCategory category, Item icon, Research... parents) {
        this(new ResourceLocation(FHMain.MODID, path), category, icon, parents);
    }

    public Research(ResourceLocation id, ResearchCategory category, Item icon, Research... parents) {
        this.id = id;
        for (Research parent : parents) this.parents.add(parent);
        this.name = new TranslationTextComponent("research."+id.getNamespace() + "." + id.getPath() + ".name");
        this.desc = new TranslationTextComponent("research."+id.getNamespace() + "." + id.getPath() + ".desc");
        this.icon = icon;
        this.category = category;
    }
    public ResourceLocation getId() {
        return id;
    }

    public void setId(ResourceLocation id) {
        this.id = id;
    }

    public HashSet<Research> getParents() {
        return parents;
    }

    public void setParents(Research... parents) {
        HashSet<Research> newSet = new HashSet<>();
        for (Research parent : parents) newSet.add(parent);
        this.parents = newSet;
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
    public ResearchData getData(UUID team) {
    	return ResearchDataManager.INSTANCE.getData(team).getData(this);
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

	public boolean isUnlocked() {
        for (Research parent : this.getParents()) {
            if (!parent.getData().isCompleted()) {
                return false;
            }
        }
        return true;
    }
}
