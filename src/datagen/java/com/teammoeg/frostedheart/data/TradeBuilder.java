package com.teammoeg.frostedheart.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.trade.BaseData;
import com.teammoeg.frostedheart.trade.BasicPolicyGroup;
import com.teammoeg.frostedheart.trade.DemandData;
import com.teammoeg.frostedheart.trade.ExtendPolicyGroup;
import com.teammoeg.frostedheart.trade.PolicyCondition;
import com.teammoeg.frostedheart.trade.PolicyGroup;
import com.teammoeg.frostedheart.trade.ProductionData;
import com.teammoeg.frostedheart.trade.TradePolicy;
import com.teammoeg.frostedheart.trade.conditions.LevelCondition;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.data.IFinishedRecipe;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;

public class TradeBuilder implements IFinishedRecipe{
	private List<PolicyGroup> groups=new ArrayList<>();
	private ResourceLocation name;
	private ResourceLocation id;
	private int weight;
	private VillagerProfession prof;
	public static class GroupBuilder{
		private List<PolicyCondition> conditions=new ArrayList<>();
		private List<BaseData> bdata=new ArrayList<>();
		private TradeBuilder parent;
		public GroupBuilder(TradeBuilder parent) {
			super();
			this.parent = parent;
		}
		public GroupBuilder level(int lvl) {
			conditions.add(new LevelCondition(lvl));
			return this;
		}
		public GroupBuilder buy(String id,int maxstore,float recover,int price,Ingredient item) {
			bdata.add(new DemandData(id,maxstore,recover,price,item));
			return this;
		}
		public GroupBuilder buy(int maxstore,float recover,int price,ItemStack item) {
			return this.buy(item.getItem().getRegistryName().toString(),maxstore, recover, price,Ingredient.fromStacks(item));
		}
		public GroupBuilder buy(int maxstore,float recover,int price,Item item) {
			return this.buy(item.getRegistryName().toString(),maxstore, recover, price,Ingredient.fromItems(item));
		}
		public GroupBuilder sell(String id,int maxstore,float recover,int price,ItemStack item) {
			bdata.add(new ProductionData(id,maxstore,recover,price,item));
			return this;
		}
		public GroupBuilder sell(int maxstore,float recover,int price,ItemStack item) {
			return this.sell(item.getItem().getRegistryName().toString(),maxstore, recover, price,item);
		}
		public GroupBuilder sell(int maxstore,float recover,int price,Item item) {
			return this.sell(item.getRegistryName().toString(),maxstore, recover, price,new ItemStack(item));
		}
		public GroupBuilder sell(int maxstore,float recover,int price,Item item,int count) {
			return this.sell(item.getRegistryName().toString(),maxstore, recover, price,new ItemStack(item,count));
		}
		public GroupBuilder basic() {
			parent.groups.add(new BasicPolicyGroup(conditions,bdata));
			return this;
		}
		public GroupBuilder extend(ResourceLocation rl) {
			parent.groups.add(new ExtendPolicyGroup(conditions,rl));
			return this;
		}
		public GroupBuilder extend(String name) {
			return this.extend(new ResourceLocation(FHMain.MODID,name));
		}
		public GroupBuilder extend(TradePolicy name) {
			return this.extend(name.getId());
		}
		public TradeBuilder finish() {
			return parent;
		}
	}
	public GroupBuilder group() {
		return new GroupBuilder(this);
	}
	public TradeBuilder id(ResourceLocation id) {
		this.id=id;
		return this;
	}
	public TradeBuilder id(String id) {
		return this.id(new ResourceLocation(FHMain.MODID,id));
	}
	public TradeBuilder name(ResourceLocation name) {
		this.name=name;
		return this;
	}
	public TradeBuilder name(String name) {
		return this.name(new ResourceLocation(FHMain.MODID,name));
	}
	public TradeBuilder weight(int weight) {
		this.weight=weight;
		return this;
	}
	public TradeBuilder profession(VillagerProfession prof) {
		this.prof=prof;
		return this;
	}
	public void finish(Consumer<IFinishedRecipe> out) {
		out.accept(this);
	}
	@Override
	public ResourceLocation getAdvancementID() {
		return null;
	}
	@Override
	public JsonObject getAdvancementJson() {
		return null;
	}
	@Override
	public ResourceLocation getID() {
		return id;
	}
	@Override
	public IRecipeSerializer<?> getSerializer() {
		return TradePolicy.SERIALIZER.get();
	}
	@Override
	public void serialize(JsonObject arg0) {
		if(name!=null)
			arg0.addProperty("name",name.toString());
		arg0.add("policies",SerializeUtil.toJsonList(groups,PolicyGroup::serialize));
        if(weight>0)
        	arg0.addProperty("weight", weight);
        if(prof!=null&&prof!=VillagerProfession.NONE)
        	arg0.addProperty("profession",prof.getRegistryName().toString());
	}
	
}
