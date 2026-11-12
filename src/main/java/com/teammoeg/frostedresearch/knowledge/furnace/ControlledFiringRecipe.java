package com.teammoeg.frostedresearch.knowledge.furnace;

import com.google.gson.*;
import com.teammoeg.frostedresearch.FRContents;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Optional;

/** Small, counted kiln batches. Experiment identity is content, not a hardcoded pack recipe ID. */
public record ControlledFiringRecipe(ResourceLocation id, ItemStack input, ItemStack fuel, int temperatureMin,
        int temperatureMax, int duration, ItemStack result, ItemStack failure, Optional<ResourceLocation> experiment)
        implements Recipe<Container> {
    public ControlledFiringRecipe {
        input=input.copy(); fuel=fuel.copy(); result=result.copy(); failure=failure.copy();
        if(input.isEmpty() || fuel.isEmpty() || result.isEmpty() || failure.isEmpty() || duration<1
            || temperatureMin<600 || temperatureMax>1400 || temperatureMin>temperatureMax)
            throw new IllegalArgumentException("Invalid controlled firing batch");
        if(!new ResourceLocation("frostedheart:fire_clay_ball").equals(ForgeRegistries.ITEMS.getKey(input.getItem()))
            || !new ResourceLocation("minecraft:charcoal").equals(ForgeRegistries.ITEMS.getKey(fuel.getItem()))
            || !new ResourceLocation("frostedheart:refractory_brick").equals(ForgeRegistries.ITEMS.getKey(result.getItem()))
            || failure.getItem()!=FRContents.Items.CRACKED_REFRACTORY_BRICK.get()
            || result.getCount()!=input.getCount() || failure.getCount()!=input.getCount())
            throw new IllegalArgumentException("Controlled furnace only fires ordinary refractory clay batches");
    }
    @Override public ItemStack input(){return input.copy();}
    @Override public ItemStack fuel(){return fuel.copy();}
    @Override public ItemStack result(){return result.copy();}
    @Override public ItemStack failure(){return failure.copy();}
    public ItemStack outcome(int temperature){return (temperature>=temperatureMin && temperature<=temperatureMax ? result : failure).copy();}
    @Override public boolean matches(Container c,Level l){return c.getContainerSize()>=2 && matches(c.getItem(0),c.getItem(1));}
    public boolean matches(ItemStack a,ItemStack b){return a.is(input.getItem()) && a.getCount()>=input.getCount() && b.is(fuel.getItem()) && b.getCount()>=fuel.getCount();}
    @Override public ItemStack assemble(Container c,RegistryAccess r){return result.copy();}
    @Override public boolean canCraftInDimensions(int w,int h){return true;}
    @Override public ItemStack getResultItem(RegistryAccess r){return result.copy();}
    @Override public ResourceLocation getId(){return id;}
    @Override public RecipeType<?> getType(){return FRContents.Recipes.CONTROLLED_FIRING.get();}
    @Override public RecipeSerializer<?> getSerializer(){return FRContents.Recipes.CONTROLLED_FIRING_SERIALIZER.get();}
    @Override public boolean isSpecial(){return true;}
    public static final class Serializer implements RecipeSerializer<ControlledFiringRecipe> {
        private static ItemStack stack(JsonObject root,String name){
            JsonObject o=GsonHelper.getAsJsonObject(root,name);
            ResourceLocation id=new ResourceLocation(GsonHelper.getAsString(o,"item"));
            var item=ForgeRegistries.ITEMS.getValue(id);int count=GsonHelper.getAsInt(o,"count",1);
            if(item==null || count<1 || count>item.getMaxStackSize()) throw new JsonSyntaxException("Invalid counted item: "+id);
            return new ItemStack(item,count);
        }
        @Override public ControlledFiringRecipe fromJson(ResourceLocation id,JsonObject o){
            return new ControlledFiringRecipe(id,stack(o,"input"),stack(o,"fuel"),GsonHelper.getAsInt(o,"temperature_min"),
                GsonHelper.getAsInt(o,"temperature_max"),GsonHelper.getAsInt(o,"duration"),stack(o,"result"),stack(o,"failure"),
                o.has("experiment") ? Optional.of(new ResourceLocation(GsonHelper.getAsString(o,"experiment"))) : Optional.empty());
        }
        @Override public ControlledFiringRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){
            return new ControlledFiringRecipe(id,b.readItem(),b.readItem(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readItem(),b.readItem(),b.readOptional(FriendlyByteBuf::readResourceLocation));
        }
        @Override public void toNetwork(FriendlyByteBuf b,ControlledFiringRecipe r){
            b.writeItem(r.input);b.writeItem(r.fuel);b.writeVarInt(r.temperatureMin);b.writeVarInt(r.temperatureMax);b.writeVarInt(r.duration);
            b.writeItem(r.result);b.writeItem(r.failure);b.writeOptional(r.experiment,FriendlyByteBuf::writeResourceLocation);
        }
    }
}
