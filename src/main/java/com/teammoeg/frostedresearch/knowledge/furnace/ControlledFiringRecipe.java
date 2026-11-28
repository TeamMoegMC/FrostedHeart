package com.teammoeg.frostedresearch.knowledge.furnace;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.teammoeg.frostedresearch.FRContents;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** A single specimen result for a temperature and duration range. */
public record ControlledFiringRecipe(ResourceLocation id, Ingredient input, int temperatureMin, int temperatureMax,
        int duration, int durationMax, ItemStack result) implements Recipe<Container> {
    public ControlledFiringRecipe {
        if (input == Ingredient.EMPTY || result.getCount() != 1 || duration < 0
                || temperatureMin < ControlledFurnaceBlockEntity.MIN_TEMPERATURE
                || temperatureMax > ControlledFurnaceBlockEntity.MAX_TEMPERATURE || temperatureMin > temperatureMax
                || durationMax < 0 || (durationMax > 0 && durationMax <= duration))
            throw new IllegalArgumentException("Invalid controlled firing recipe: " + id);
        result = result.copy();
    }

    @Override public ItemStack result() { return result.copy(); }
    public boolean matches(ItemStack stack) { return !stack.isEmpty() && input.test(stack); }
    public boolean matches(ItemStack stack, int temperature) {
        return matches(stack) && temperature >= temperatureMin && temperature <= temperatureMax;
    }
    @Override public boolean matches(Container c, Level l) { return c.getContainerSize() > 0 && matches(c.getItem(0)); }
    @Override public ItemStack assemble(Container c, RegistryAccess r) { return result(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ItemStack getResultItem(RegistryAccess r) { return result(); }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeType<?> getType() { return FRContents.Recipes.CONTROLLED_FIRING.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return FRContents.Recipes.CONTROLLED_FIRING_SERIALIZER.get(); }
    @Override public boolean isSpecial() { return true; }

    public static final class Serializer implements RecipeSerializer<ControlledFiringRecipe> {
        private static ItemStack stack(JsonObject root, String name) {
            JsonObject data = GsonHelper.getAsJsonObject(root, name);
            ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(data, "item"));
            var item = ForgeRegistries.ITEMS.getValue(id);
            if (item == null || GsonHelper.getAsInt(data, "count", 1) != 1)
                throw new JsonSyntaxException("Controlled firing requires one item for " + name + ": " + id);
            return new ItemStack(item);
        }

        @Override public ControlledFiringRecipe fromJson(ResourceLocation id, JsonObject data) {
            int durationMax = GsonHelper.getAsInt(data, "duration_max", 0);
            if (data.has("duration_max") && durationMax <= 0)
                throw new JsonSyntaxException("Controlled firing duration_max must be positive: " + id);
            JsonObject input = GsonHelper.getAsJsonObject(data, "input");
            if (GsonHelper.getAsInt(input, "count", 1) != 1)
                throw new JsonSyntaxException("Controlled firing requires one specimen for input: " + id);
            return new ControlledFiringRecipe(id, Ingredient.fromJson(input), GsonHelper.getAsInt(data, "temperature_min"),
                    GsonHelper.getAsInt(data, "temperature_max"), GsonHelper.getAsInt(data, "duration"),
                    durationMax, stack(data, "result"));
        }

        @Override public ControlledFiringRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new ControlledFiringRecipe(id, Ingredient.fromNetwork(buffer), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readItem());
        }

        @Override public void toNetwork(FriendlyByteBuf buffer, ControlledFiringRecipe recipe) {
            recipe.input.toNetwork(buffer);
            buffer.writeVarInt(recipe.temperatureMin);
            buffer.writeVarInt(recipe.temperatureMax);
            buffer.writeVarInt(recipe.duration);
            buffer.writeVarInt(recipe.durationMax);
            buffer.writeItem(recipe.result);
        }
    }
}
