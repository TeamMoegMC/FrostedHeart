package com.teammoeg.frostedheart.research;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.HashSet;

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
    private HashSet<IClue> clues=new HashSet<>();
    private ResearchCategory category;
    private ArrayList<ItemStack> requireItems=new ArrayList<>();
    private int points;

    public Research(String path, Research... parents) {
        this(new ResourceLocation(FHMain.MODID, path), Items.GRASS_BLOCK, parents);
    }

    public Research(String path, Item icon, Research... parents) {
        this(new ResourceLocation(FHMain.MODID, path), icon, parents);
    }

    public Research(ResourceLocation id, Item icon, Research... parents) {
        this.id = id;
        for (Research parent : parents) this.parents.add(parent);
        this.name = new TranslationTextComponent("research."+id.getNamespace() + "." + id.getPath() + ".name");
        this.desc = new TranslationTextComponent("research."+id.getNamespace() + "." + id.getPath() + ".desc");
        this.icon = icon;
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

    public void setCategory(ResearchCategory category) {
        this.category = category;
    }

    public String toString() {
        return "Research[" + id + "]";
    }
}
