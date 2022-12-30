package com.teammoeg.frostedheart.trade;

import java.util.List;
import java.util.Map;

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
import net.minecraftforge.fml.RegistryObject;

public class TradePolicy  extends IESerializableRecipe{
	public static IRecipeType<TradePolicy> TYPE;
    public static RegistryObject<IERecipeSerializer<TradePolicy>> SERIALIZER;
    public static Map<ResourceLocation,TradePolicy> policies;
    private ResourceLocation name;
    List<PolicyGroup> groups;

	public TradePolicy(ResourceLocation id, ResourceLocation name,List<PolicyGroup> groups) {
		super(ItemStack.EMPTY,TYPE, id);
		this.name = name;
		this.groups = groups;
	}


	@Override
	public ItemStack getRecipeOutput() {
		return ItemStack.EMPTY;
	}

	@Override
	protected IERecipeSerializer<TradePolicy> getIESerializer() {
		return SERIALIZER.get();
	}
	public PolicySnapshot get(VillagerEntity ve) {
		PolicySnapshot ps=new PolicySnapshot();
		this.CollectPolicies(ps, ve);
		return ps;
	}
	public void CollectPolicies(PolicySnapshot policy,VillagerEntity ve) {
		groups.forEach(t->t.CollectPolicies(policy,ve));
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
            return new TradePolicy(recipeId,name,groups);
        }

        @Nullable
        @Override
        public TradePolicy read(ResourceLocation recipeId, PacketBuffer buffer) {
        	ResourceLocation name=SerializeUtil.readOptional(buffer,PacketBuffer::readResourceLocation).orElse(null);
            List<PolicyGroup> groups=SerializeUtil.readList(buffer,PolicyGroup::read);
            return new TradePolicy(recipeId,name,groups);
        }

        @Override
        public void write(PacketBuffer buffer, TradePolicy recipe) {
        	SerializeUtil.writeOptional2(buffer, recipe.name,PacketBuffer::writeResourceLocation);
        	SerializeUtil.writeList(buffer,recipe.groups,PolicyGroup::write);
        }
    }
	public ResourceLocation getName() {
		return name==null?super.id:name;
	}
}
