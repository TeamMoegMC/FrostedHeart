package com.teammoeg.frostedheart.trade;

import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.util.SerializeUtil;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.WeightedRandom;
import net.minecraftforge.fml.RegistryObject;

public class TradePolicy  extends IESerializableRecipe{
	public static IRecipeType<TradePolicy> TYPE;
    public static RegistryObject<IERecipeSerializer<TradePolicy>> SERIALIZER;
    public static Map<ResourceLocation,TradePolicy> policies;
    public static class Weighted extends WeightedRandom.Item{
    	TradePolicy policy;

		public Weighted(int itemWeightIn, TradePolicy policy) {
			super(itemWeightIn);
			this.policy = policy;
		}
    }
    public static int totalW;
    public static List<Weighted> items;
    private ResourceLocation name;
    List<PolicyGroup> groups;
    int weight=0;

	public TradePolicy(ResourceLocation id, ResourceLocation name,List<PolicyGroup> groups,int weight) {
		super(ItemStack.EMPTY,TYPE, id);
		this.name = name;
		this.groups = groups;
		this.weight=weight;
		
	}


	@Override
	public ItemStack getRecipeOutput() {
		return ItemStack.EMPTY;
	}
	public Weighted asWeight() {
		if(weight>0)
		return new Weighted(weight,this);
		return null;
	}
	@Override
	protected IERecipeSerializer<TradePolicy> getIESerializer() {
		return SERIALIZER.get();
	}
	public PolicySnapshot get(FHVillagerData ve) {
		PolicySnapshot ps=new PolicySnapshot();
		this.CollectPolicies(ps, ve);
		return ps;
	}
	public void CollectPolicies(PolicySnapshot policy,FHVillagerData ve) {
		groups.forEach(t->t.CollectPolicies(policy,ve));
	}
	public static TradePolicy random(Random rnd) {
		return WeightedRandom.getRandomItem(rnd, items,totalW).policy;
	}
	public static class Serializer extends IERecipeSerializer<TradePolicy> {
        @Override
        public ItemStack getIcon() {
            return new ItemStack(Items.VILLAGER_SPAWN_EGG);
        }

        @Override
        public TradePolicy readFromJson(ResourceLocation recipeId, JsonObject json) {
            ResourceLocation name=json.has("name")?new ResourceLocation(json.get("name").getAsString()):null;
            List<PolicyGroup> groups=SerializeUtil.parseJsonList(json.get("policies"),PolicyGroup::read);
            int root=json.has("weight")?json.get("weight").getAsInt():0;
            return new TradePolicy(recipeId,name,groups,root);
        }

        @Nullable
        @Override
        public TradePolicy read(ResourceLocation recipeId, PacketBuffer buffer) {
        	ResourceLocation name=SerializeUtil.readOptional(buffer,PacketBuffer::readResourceLocation).orElse(null);
            List<PolicyGroup> groups=SerializeUtil.readList(buffer,PolicyGroup::read);
            int root=buffer.readVarInt();
            return new TradePolicy(recipeId,name,groups,root);
        }

        @Override
        public void write(PacketBuffer buffer, TradePolicy recipe) {
        	SerializeUtil.writeOptional2(buffer, recipe.name,PacketBuffer::writeResourceLocation);
        	SerializeUtil.writeList(buffer,recipe.groups,PolicyGroup::write);
        	buffer.writeVarInt(recipe.weight);
        }
    }
	public ResourceLocation getName() {
		return name==null?super.id:name;
	}
}
