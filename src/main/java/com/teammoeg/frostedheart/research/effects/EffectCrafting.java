package com.teammoeg.frostedheart.research.effects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.ResearchDataManager;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.research.gui.FHIcons;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistries;

public class EffectCrafting extends Effect{
	List<IRecipe<?>> unlocks=new ArrayList<>();
	ItemStack itemStack=null;
	Item item=null;
    public EffectCrafting(ItemStack item) {
    	super();
    
    	this.itemStack=item;
    	for(IRecipe<?> r:ResearchDataManager.server.getRecipeManager().getRecipes()) {
    		if(r.getRecipeOutput().equals(item)) {
    			unlocks.add(r);
    		}
    	}
    }
    public EffectCrafting(IItemProvider item) {
    	super();
    	this.item=item.asItem();
    	initItem();
    }
    private void initItem() {
    	for(IRecipe<?> r:ResearchDataManager.server.getRecipeManager().getRecipes()) {
    		if(r.getRecipeOutput().getItem().equals(this.item)) {
    			unlocks.add(r);
    		}
    	}
    }
    public EffectCrafting(ResourceLocation recipe) {
    	super("@gui." + FHMain.MODID + ".effect.crafting",new ArrayList<>());
    	Optional<? extends IRecipe<?>> r=ResearchDataManager.server.getRecipeManager().getRecipe(recipe);
    	
    	if(r.isPresent()) {
    		unlocks.add(r.get());
    	}
    }
    public EffectCrafting(JsonObject jo) {
    	super(jo);
    	if(jo.has("item")) {
    		JsonElement je=jo.get("item");
    		if(je.isJsonPrimitive())
    			item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(je.getAsString()));
    		else
    			itemStack=SerializeUtil.fromJson(je);
    	}else if(jo.has("recipes")) {
    		unlocks=SerializeUtil.parseJsonElmList(jo.get("recipes"),e->ResearchDataManager.server.getRecipeManager().getRecipe(new ResourceLocation(e.getAsString())).orElse(null));
    		unlocks.removeIf(Objects::isNull);
    	}
    }

	public EffectCrafting(PacketBuffer pb) {
		super(pb);
		item=SerializeUtil.readOptional(pb,p->p.readRegistryIdUnsafe(ForgeRegistries.ITEMS)).orElse(null);
		if(item==null) {
			unlocks=SerializeUtil.readList(pb,p->ResearchDataManager.server.getRecipeManager().getRecipe(p.readResourceLocation()).orElse(null));
			unlocks.removeIf(Objects::isNull);
		}else initItem();
	}
	@Override
    public void init() {
		ResearchListeners.recipe.addAll(unlocks);
    }

    @Override
    public boolean grant(TeamResearchData team, PlayerEntity triggerPlayer) {
    	team.crafting.addAll(unlocks);
		return true;
    }

    @Override
    public void revoke(TeamResearchData team) {
    	team.crafting.addAll(unlocks);
    }

	@Override
	public String getId() {
		return "recipe";
	}
	@Override
	public JsonObject serialize() {
		JsonObject jo=super.serialize();
		if(item!=null)
			jo.addProperty("item", item.getRegistryName().toString());
		else if(itemStack!=null)
			jo.add("item",SerializeUtil.toJson(itemStack));
		else if(unlocks.size()==1)
			jo.addProperty("recipes",unlocks.get(1).getId().toString());
		else if(unlocks.size()>1)
			jo.add("recipes",SerializeUtil.toJsonStringList(unlocks,IRecipe<?>::getId));
		return jo;
	}
	@Override
	public void write(PacketBuffer buffer) {
		super.write(buffer);
		SerializeUtil.writeOptional(buffer,item,(o,b)->b.writeRegistryIdUnsafe(ForgeRegistries.ITEMS,o));
		if(item==null)
			SerializeUtil.writeList(buffer,unlocks,(o,b)->b.writeResourceLocation(o.getId()));
	}
	@Override
	public int getIntID() {
		return 2;
	}
	@Override
	public FHIcon getDefaultIcon() {
		if(item!=null)
			return FHIcons.getIcon(FHIcons.getIcon(item),FHIcons.getIcon(Items.CRAFTING_TABLE));
		else if(itemStack!=null)
			return FHIcons.getIcon(FHIcons.getIcon(itemStack),FHIcons.getIcon(Items.CRAFTING_TABLE));
		else {
			Set<ItemStack> stacks=new HashSet<>();
			for(IRecipe<?> r:unlocks) {
				if(!r.getRecipeOutput().isEmpty()) {
					stacks.add(r.getRecipeOutput());
				}
			}
			if(!stacks.isEmpty())
				return FHIcons.getIcon(FHIcons.getStackIcons(stacks),FHIcons.getIcon(Items.CRAFTING_TABLE));
		}
		return FHIcons.getIcon(FHIcons.getIcon(TechIcons.Question),FHIcons.getIcon(Items.CRAFTING_TABLE));
	}
	@Override
	public IFormattableTextComponent getDefaultName() {
		return GuiUtils.translateGui("effect.crafting");
	}
	@Override
	public List<ITextComponent> getDefaultTooltip() {
		List<ITextComponent> tooltip=new ArrayList<>();
		
		if(item!=null)
			tooltip.add(new TranslationTextComponent(item.getTranslationKey()));
		else if(itemStack!=null)
			tooltip.add(itemStack.getDisplayName());
		else {
			Set<ItemStack> stacks=new HashSet<>();
			for(IRecipe<?> r:unlocks) {
				if(!r.getRecipeOutput().isEmpty()) {
					stacks.add(r.getRecipeOutput());
				}
			}
			if(stacks.isEmpty())
				tooltip.add(GuiUtils.translateGui("effect.recipe.error"));
			else
				for(ItemStack is:stacks) {
					tooltip.add(is.getDisplayName());
				}
		}
		
		return tooltip;
	}

}
