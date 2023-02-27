package com.teammoeg.frostedheart.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.trade.policy.BaseData;
import com.teammoeg.frostedheart.trade.policy.BasicPolicyGroup;
import com.teammoeg.frostedheart.trade.policy.DemandData;
import com.teammoeg.frostedheart.trade.policy.ExtendPolicyGroup;
import com.teammoeg.frostedheart.trade.policy.NopData;
import com.teammoeg.frostedheart.trade.policy.PolicyAction;
import com.teammoeg.frostedheart.trade.policy.PolicyCondition;
import com.teammoeg.frostedheart.trade.policy.PolicyGroup;
import com.teammoeg.frostedheart.trade.policy.ProductionData;
import com.teammoeg.frostedheart.trade.policy.TradePolicy;
import com.teammoeg.frostedheart.trade.policy.actions.AddFlagValueAction;
import com.teammoeg.frostedheart.trade.policy.actions.SetFlagAction;
import com.teammoeg.frostedheart.trade.policy.actions.SetFlagValueAction;
import com.teammoeg.frostedheart.trade.policy.conditions.FlagValueCondition;
import com.teammoeg.frostedheart.trade.policy.conditions.GreaterFlagCondition;
import com.teammoeg.frostedheart.trade.policy.conditions.LevelCondition;
import com.teammoeg.frostedheart.trade.policy.conditions.NotCondition;
import com.teammoeg.frostedheart.trade.policy.conditions.TotalTradeCondition;
import com.teammoeg.frostedheart.trade.policy.conditions.WithFlagCondition;
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
	public static class ConditionBuilder<T>{
		Consumer<PolicyCondition> consumer;
		T parent;
		public ConditionBuilder(Consumer<PolicyCondition> consumer, T parent) {
			super();
			this.consumer = consumer;
			this.parent = parent;
		}
		public ConditionBuilder<T> level(int lvl) {
			return condition(new LevelCondition(lvl));
		}
		public ConditionBuilder<T> lowerLevel(int lvl) {
			return not(new LevelCondition(lvl));
		}
		public ConditionBuilder<T> not(PolicyCondition pc) {
			return condition(new NotCondition(pc));
		}
		public ConditionBuilder<T> total(int val) {
			return condition(new TotalTradeCondition(val));
		}
		public ConditionBuilder<T> lowerTotal(int val) {
			return not(new TotalTradeCondition(val));
		}
		public ConditionBuilder<T> hasFlag(String flag) {
			return condition(new WithFlagCondition(flag));
		}
		public ConditionBuilder<T> hasNoFlag(String flag) {
			return not(new WithFlagCondition(flag));
		}
		public ConditionBuilder<T> hasFlag(String flag,int val) {
			return condition(new FlagValueCondition(flag,val));
		}
		public ConditionBuilder<T> hasNoFlag(String flag,int val) {
			return not(new FlagValueCondition(flag,val));
		}
		public ConditionBuilder<T> greaterFlag(String flag,int val) {
			return condition(new GreaterFlagCondition(flag,val));
		}
		public ConditionBuilder<T> lesserFlag(String flag,int val) {
			return not(new GreaterFlagCondition(flag,val));
		}
		public ConditionBuilder<T> condition(PolicyCondition pc) {
			consumer.accept(pc);
			return this;
		}
		public T finish() {
			return parent;
		}
	}
	public static class ActionBuilder<T>{
		Consumer<PolicyAction> consumer;
		T parent;
		public ActionBuilder(Consumer<PolicyAction> consumer, T parent) {
			super();
			this.consumer = consumer;
			this.parent = parent;
		}
		public ActionBuilder<T> addFlag(String name,int val) {
			action(new AddFlagValueAction(name,val));
			return this;
		}
		public ActionBuilder<T> setFlag(String name) {
			action(new SetFlagAction(name));
			return this;
		}
		public ActionBuilder<T> unsetFlag(String name) {
			action(new SetFlagValueAction(name,0));
			return this;
		}
		public ActionBuilder<T> setFlag(String name,int val) {
			action(new SetFlagValueAction(name,val));
			return this;
		}
		public ActionBuilder<T> action(PolicyAction act){
			consumer.accept(act);
			return this;
		}
		public T finish() {
			return parent;
		}
	}
	public static class GroupBuilder{
		private List<PolicyCondition> conditions=new ArrayList<>();
		private List<BaseData> bdata=new ArrayList<>();
		private TradeBuilder parent;
		BaseData lastAction;
		public GroupBuilder(TradeBuilder parent) {
			super();
			this.parent = parent;
		}
		public ConditionBuilder<GroupBuilder> groupCondition(){
			return new ConditionBuilder<>(conditions::add,this);
		}
		public ConditionBuilder<GroupBuilder> restocksBy(){
			return new ConditionBuilder<>(lastAction.restockconditions::add,this);
		}
		public ActionBuilder<GroupBuilder> restockAction(){
			return new ActionBuilder<>(lastAction.actions::add,this);
		}
		public ActionBuilder<GroupBuilder> useAction(){
			return new ActionBuilder<>(lastAction.soldactions::add,this);
		}
		public GroupBuilder buy(String id,int maxstore,float recover,int price,Ingredient item) {
			bdata.add(lastAction=new DemandData(id,maxstore,recover,price,item));
			return this;
		}
		public GroupBuilder buy(int maxstore,float recover,int price,ItemStack item) {
			return this.buy(item.getItem().getRegistryName().toString(),maxstore, recover, price,Ingredient.fromStacks(item));
		}
		public GroupBuilder buy(int maxstore,float recover,int price,Item item) {
			return this.buy(item.getRegistryName().toString(),maxstore, recover, price,Ingredient.fromItems(item));
		}
		public GroupBuilder sell(String id,int maxstore,float recover,int price,ItemStack item) {
			bdata.add(lastAction=new ProductionData(id,maxstore,recover,price,item));
			return this;
		}
		public GroupBuilder nop(String id,int maxstore,float recover,int price) {
			bdata.add(lastAction=new NopData(id,maxstore,recover,price));
			return this;
		}
		public GroupBuilder nop(String id) {
			return nop(id,1,0,0);
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
