package com.teammoeg.frostedheart.content.recipes;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;
import top.theillusivec4.diet.api.IDietGroup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DietValueRecipe extends IESerializableRecipe {
    public static IRecipeType<DietValueRecipe> TYPE;
    public static RegistryObject<IERecipeSerializer<DietValueRecipe>> SERIALIZER;
    final Map<String, Float> groups;
    Map<IDietGroup, Float> cache;
    public final Item item;

    public DietValueRecipe(ResourceLocation id, Item it) {
        this(id, new HashMap<>(), it);
    }

    public static Map<Item, DietValueRecipe> recipeList = Collections.emptyMap();

    public DietValueRecipe(ResourceLocation id, Map<String, Float> groups, Item it) {
        super(ItemStack.EMPTY, TYPE, id);
        this.groups = groups;
        this.item = it;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    public Map<IDietGroup, Float> getValues() {
        if (cache == null)
            cache = groups.entrySet().stream().collect(Collectors.toMap(e -> DietGroupCodec.getGroup(e.getKey()), e -> e.getValue()));
        return cache;
    }

    @Override
    protected IERecipeSerializer getIESerializer() {
        return SERIALIZER.get();
    }

    @Override
    public String toString() {
        return "DietValueRecipe [groups=" + groups + ", item=" + item + "]";
    }


}
