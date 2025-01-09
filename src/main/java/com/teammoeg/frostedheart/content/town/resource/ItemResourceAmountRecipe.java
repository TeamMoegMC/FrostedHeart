package com.teammoeg.frostedheart.content.town.resource;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IERecipeTypes;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHBlocks;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class ItemResourceAmountRecipe extends IESerializableRecipe {
    public static RegistryObject<RecipeType<ItemResourceAmountRecipe>> TYPE;
    public static RegistryObject<IERecipeSerializer<ItemResourceAmountRecipe>> SERIALIZER;
    public static Lazy<IERecipeTypes.TypeWithClass<ItemResourceAmountRecipe>> IEType = Lazy.of(() -> new IERecipeTypes.TypeWithClass<>(TYPE, ItemResourceAmountRecipe.class));
    public final ItemStack item;
    public final TagKey<Item> itemOfResourceKey;
    public final float amount;

    public ItemResourceAmountRecipe(ResourceLocation id, ItemStack item, TagKey<Item> itemOfResourceKey, float amount) {
        super(Lazy.of(() -> ItemStack.EMPTY), IEType.get(), id);
        this.item = item;
        this.itemOfResourceKey = itemOfResourceKey;
        this.amount = amount;
    }

    @Override
    protected IERecipeSerializer<?> getIESerializer() {
        return null;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    public static class Serializer extends IERecipeSerializer<ItemResourceAmountRecipe> {
        @Override
        public ItemStack getIcon() {
            return new ItemStack(FHBlocks.WAREHOUSE.get());
        }

        @Nullable
        @Override
        public ItemResourceAmountRecipe fromNetwork(@NotNull ResourceLocation recipeId, FriendlyByteBuf buffer) {
            ItemStack item = buffer.readItem();
            TagKey<Item> itemOfResourceKey = ItemTags.create(buffer.readResourceLocation());
            float amount = buffer.readFloat();
            return new ItemResourceAmountRecipe(recipeId, item, itemOfResourceKey, amount);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ItemResourceAmountRecipe recipe) {
            buffer.writeItem(recipe.item);
            buffer.writeResourceLocation(recipe.itemOfResourceKey.location());
            buffer.writeFloat(recipe.amount);
        }

        @Override
        public ItemResourceAmountRecipe readFromJson(ResourceLocation recipeId, JsonObject json, ICondition.IContext ctx) {
            ItemStack item = readOutput(json.get("item")).get();
            TagKey<Item> itemOfResourceKey = ItemTags.create(new ResourceLocation(GsonHelper.getAsString(json, "itemOfResourceKey")));
            float amount = GsonHelper.getAsFloat(json, "amount");
            return new ItemResourceAmountRecipe(recipeId, item, itemOfResourceKey, amount);
        }


    }
}
