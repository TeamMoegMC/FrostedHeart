package com.teammoeg.frostedheart.base.item.rankine.alloys;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.base.item.rankine.init.RankineItems;
import com.teammoeg.frostedheart.base.item.rankine.init.RankineRecipeSerializers;
import com.teammoeg.frostedheart.base.item.rankine.init.RankineRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ElementRecipe implements Recipe<Container> {

    private final ResourceLocation id;
    private final String name;
    private final String symbol;
    private final int num;
    private final int color;
    private final float potential;
    private final List<String> items;
    private final List<Integer> values;
    private final List<ElementEquation> stats;
    private final List<String> enchantments;
    private final List<String> enchantmentTypes;
    private final List<Float> enchantmentFactors;

    public ElementRecipe(ResourceLocation idIn, String nameIn, String symbolIn, int numIn, int colorIn, float potentialIn, List<String> items, List<Integer> values, List<ElementEquation> statsIn,
                         List<String> enchantmentsIn, List<String> enchantmentTypesIn, List<Float> enchantmentFactorsIn) {
        this.id = idIn;
        this.num = numIn;
        this.color = colorIn;
        this.potential = potentialIn;
        this.items = items;
        this.values = values;
        this.name = nameIn;
        this.symbol = symbolIn;
        this.stats = statsIn;
        this.enchantments = enchantmentsIn;
        this.enchantmentTypes = enchantmentTypesIn;
        this.enchantmentFactors = enchantmentFactorsIn;
    }

    @Override
    public boolean matches(Container inv, Level worldIn) {
        Item reg = inv.getItem(0).getItem();
        if (reg != Items.AIR) {
            for (String s : items) {
                if (s.contains("T#")) {
                    TagKey<Item> tag = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(),new ResourceLocation(s.split("T#")[1]));
                    if (ForgeRegistries.ITEMS.tags().getTag(tag).contains(reg)){
                        return true;
                    }
                } else if (s.contains("I#")) {
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(s.split("I#")[1]));
                    if (item != Items.AIR && item == reg) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(Container p_44001_, RegistryAccess p_267165_) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public String getGroup() {
        return "";
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess p_267052_) {
        return new ItemStack(RankineItems.ELEMENT.get());
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.withSize(items.size(),Ingredient.EMPTY);
        int count = 0;
        for (String s : getItems()) {

            if (s.contains("T#")) {
                TagKey<Item> tag = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(),new ResourceLocation(s.split("T#")[1]));
                list.set(count,Ingredient.of(tag));
            } else if (s.contains("I#")) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(s.split("I#")[1]));
                if (item != null) {
                    list.set(count,Ingredient.of(() -> item));
                }
            }
            count++;
        }
        return list;
    }

    public String getIngredientFromCount(int count) {
        if (getValues().contains(count)) {
            return getItems().get(getValues().indexOf(count));
        }
        return "";
    }

    @Override
    public RecipeType<?> getType() {
        return RankineRecipeTypes.ELEMENT.get();
    }

    public String getName() {
        return this.name;
    }

    public String getSymbol() {
        return this.symbol;
    }

    public int getAtomicNumber() {
        return this.num;
    }


    public int getColor() {
        return this.color;
    }

    public float getElectrodePotential() {
        return potential;
    }

    public List<ElementEquation> getStats() {
        return stats;
    }

    public ElementEquation getDurabilityFormula() {
        return this.getStats().get(0);
    }

    public int getDurability(int x) { return this.getDurabilityFormula().calculateRounded(x);}

    public ElementEquation getMiningSpeedFormula() {
        return this.getStats().get(1);
    }

    public float getMiningSpeed(int x) { return this.getMiningSpeedFormula().calculateFloat(x);}

    public ElementEquation getMiningLevelFormula() {
        return this.getStats().get(2);
    }

    public int getMiningLevel(int x) { return this.getMiningLevelFormula().calculateRounded(x);}

    public ElementEquation getEnchantabilityFormula() {
        return this.getStats().get(3);
    }

    public int getEnchantability(int x) { return this.getEnchantabilityFormula().calculateRounded(x);}

    public ElementEquation getDamageFormula() {
        return this.getStats().get(4);
    }

    public float getDamage(int x) { return this.getDamageFormula().calculateFloat(x);}

    public ElementEquation getAttackSpeedFormula() {
        return this.getStats().get(5);
    }

    public float getAttackSpeed(int x) { return this.getAttackSpeedFormula().calculateFloat(x);}

    public ElementEquation getCorrosionResistanceFormula() {
        return this.getStats().get(6);
    }

    public float getCorrosionResistance(int x) { return this.getCorrosionResistanceFormula().calculateFloat(x);}

    public ElementEquation getHeatResistanceFormula() {
        return this.getStats().get(7);
    }

    public float getHeatResistance(int x) { return this.getHeatResistanceFormula().calculateFloat(x);}

    public ElementEquation getKnockbackResistanceFormula() {
        return this.getStats().get(8);
    }

    public float getKnockbackResistance(int x) { return this.getKnockbackResistanceFormula().calculateFloat(x);}

    public ElementEquation getToughnessFormula() {
        return this.getStats().get(9);
    }

    public float getToughness(int x) { return this.getToughnessFormula().calculateFloat(x);}

    public ElementEquation getStatEquation(int stat){
        return this.getStats().get(stat);
    }

    public float getStat(int stat, int x) {
        switch (stat) {
            case 0:
                return this.getDurability(x);
            case 1:
                return this.getMiningSpeed(x);
            case 2:
                return this.getMiningLevel(x);
            case 3:
                return this.getEnchantability(x);
            case 4:
                return this.getDamage(x);
            case 5:
                return this.getAttackSpeed(x);
            case 6:
                return this.getCorrosionResistance(x);
            case 7:
                return this.getHeatResistance(x);
            case 8:
                return this.getKnockbackResistance(x);
            case 9:
                return this.getToughness(x);
        }
        return -1;
    }
    public List<String> getItems() {
        return items;
    }

    public List<Integer> getValues() {
        return values;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RankineRecipeSerializers.ELEMENT_RECIPE_SERIALIZER.get();
    }

    public List<String> getEnchantments() {
        return enchantments;
    }

    public List<String> getEnchantmentTypes() {
        return enchantmentTypes;
    }

    public List<Float> getEnchantmentFactors() {
        return enchantmentFactors;
    }

    public int getMaterialCount(Item reg) {
        for (int i = 0; i < getItems().size(); i++) {
            String s = getItems().get(i);
            if (s.contains("T#")) {
                TagKey<Item> tag = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(),new ResourceLocation(s.split("T#")[1]));
                if (ForgeRegistries.ITEMS.tags().getTag(tag).contains(reg)){
                    return getValues().get(i);
                }
            } else if (s.contains("I#")) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(s.split("I#")[1]));
                if (item != Items.AIR && reg == item) {
                    return getValues().get(i);
                }
            }
        }
        return 0;
    }

    public static class Serializer implements RecipeSerializer<ElementRecipe> {
        private static final ResourceLocation NAME = new ResourceLocation("rankine", "element");

        @Override
        public ElementRecipe fromJson(ResourceLocation elementId, JsonObject json) {
            String n = json.get("name").getAsString().toLowerCase(Locale.ROOT);
            String s = json.get("symbol").getAsString();
            int t = json.get("atomic").getAsInt();
            int c;
            if (json.has("color")) {
                c = Math.max(0,json.get("color").getAsInt());
            } else {
                c = 16777215;
            }
            float p;
            if (json.has("potential")) {
                p = json.get("potential").getAsFloat();
            } else {
                p = 0;
            }
            JsonArray it = GsonHelper.getAsJsonArray(json,"items");
            JsonArray val = GsonHelper.getAsJsonArray(json,"values");
            List<String> itemList = new ArrayList<>();
            List<Integer> valueList = new ArrayList<>();
            for (int i = 0; i < it.size(); i++) {
                itemList.add(i,it.get(i).getAsString());
                valueList.add(i,val.get(i).getAsInt());
            }
            String[] stats = new String[]{"durability","miningspeed","mininglevel","enchantability","damage","attackspeed",
            "corrosionresist","heatresist","knockbackresist","toughness"};
            List<ElementEquation> equations = new ArrayList<>();
            int index = 0;
            for (String stat : stats) {
                if (json.has(stat)) {
                    JsonObject object = GsonHelper.getAsJsonObject(json, stat);
                    JsonArray breaks = GsonHelper.getAsJsonArray(object,"breaks");
                    JsonArray formulas = GsonHelper.getAsJsonArray(object,"formulas");
                    JsonArray a = GsonHelper.getAsJsonArray(object,"a");
                    JsonArray b = GsonHelper.getAsJsonArray(object,"b");
                    JsonArray modifiers = GsonHelper.getAsJsonArray(object,"modifiers");
                    JsonArray limit = GsonHelper.getAsJsonArray(object,"limit");

                    int[] breaksIn = new int[breaks.size()];
                    ElementEquation.FormulaType[] formulasIn = new ElementEquation.FormulaType[breaks.size()];
                    float[] aIn = new float[breaks.size()];
                    float[] bIn = new float[breaks.size()];
                    ElementEquation.FormulaModifier[] modifiersIn = new ElementEquation.FormulaModifier[breaks.size()];
                    float[] limitIn = new float[breaks.size()];
                    for (int i = 0; i < breaks.size(); i++) {
                        breaksIn[i] = breaks.get(i).getAsInt();
                        formulasIn[i] = ElementEquation.FormulaType.valueOf(formulas.get(i).getAsString().toUpperCase(Locale.ROOT));
                        aIn[i] = a.get(i).getAsFloat();
                        bIn[i] = b.get(i).getAsFloat();
                        modifiersIn[i] = ElementEquation.FormulaModifier.valueOf(modifiers.get(i).getAsString().toUpperCase(Locale.ROOT));
                        limitIn[i] = limit.get(i).getAsFloat();
                    }
                    equations.add(index,new ElementEquation());
                } else {
                    equations.add(index,new ElementEquation());
                }
                index++;
            }
            List<String> enchantments = new ArrayList<>();
            List<String> enchantmentTypes = new ArrayList<>();
            List<Float> enchantmentFactors = new ArrayList<>();
            if (json.has("enchantments")) {
                JsonArray e = GsonHelper.getAsJsonArray(json,"enchantments");
                JsonArray eTypes = GsonHelper.getAsJsonArray(json,"enchantmentTypes");
                JsonArray eFactors = GsonHelper.getAsJsonArray(json,"enchantmentFactors");
                for (int i = 0; i < e.size(); i++) {
                    enchantments.add(e.get(i).getAsString().toLowerCase(Locale.ROOT));
                    enchantmentTypes.add(eTypes.get(i).getAsString().toUpperCase(Locale.ROOT));
                    enchantmentFactors.add(Math.min(1,Math.max(-1,eFactors.get(i).getAsFloat())));
                }
            }
            return new ElementRecipe(elementId,n,s,t,c,p,itemList,valueList,equations,enchantments,enchantmentTypes,enchantmentFactors);
        }

        @Nullable
        @Override
        public ElementRecipe fromNetwork(ResourceLocation elementId, FriendlyByteBuf buffer) {
            List<ElementEquation> equations = new ArrayList<>();
            List<String> itemList = new ArrayList<>();
            List<Integer> valueList = new ArrayList<>();

            String name = buffer.readUtf();
            String sym = buffer.readUtf();
            int atomic = buffer.readInt();
            int color = Math.max(0,buffer.readInt());
            float potential = buffer.readFloat();

            int itemSize = buffer.readInt();
            for (int i = 0; i < itemSize; i++) {
                itemList.add(i,buffer.readUtf());
                valueList.add(i,buffer.readInt());
            }

            for (int j = 0; j < 10; j++) {
                boolean stat = buffer.readBoolean();
                if (stat) {
                    int breaks_dur = buffer.readInt();
                    int[] breaksIn = new int[breaks_dur];
                    ElementEquation.FormulaType[] formulasIn = new ElementEquation.FormulaType[breaks_dur];
                    float[] aIn = new float[breaks_dur];
                    float[] bIn = new float[breaks_dur];
                    ElementEquation.FormulaModifier[] modifiersIn = new ElementEquation.FormulaModifier[breaks_dur];
                    float[] limitIn = new float[breaks_dur];
                    for (int i = 0; i < breaks_dur; i++) {
                        breaksIn[i] = buffer.readInt();
                        formulasIn[i] = ElementEquation.FormulaType.valueOf(buffer.readUtf().toUpperCase(Locale.ROOT));
                        aIn[i] = buffer.readFloat();
                        bIn[i] = buffer.readFloat();
                        modifiersIn[i] = ElementEquation.FormulaModifier.valueOf(buffer.readUtf().toUpperCase(Locale.ROOT));
                        limitIn[i] = buffer.readFloat();
                    }
                    equations.add(j,new ElementEquation());
                } else {
                    equations.add(j,new ElementEquation());
                }
            }

            int size = buffer.readInt();
            List<String> enchantments = new ArrayList<>();
            List<String> enchantmentTypes = new ArrayList<>();
            List<Float> enchantmentFactors = new ArrayList<>();
            for (int j = 0; j < size; j++) {
                enchantments.add(buffer.readUtf().toLowerCase(Locale.ROOT));
                enchantmentTypes.add(buffer.readUtf().toUpperCase(Locale.ROOT));
                enchantmentFactors.add(buffer.readFloat());
            }

            return new ElementRecipe(elementId,name,sym,atomic,color,potential,itemList,valueList,equations,enchantments,enchantmentTypes,enchantmentFactors);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ElementRecipe element) {
            buffer.writeUtf(element.getName());
            buffer.writeUtf(element.getSymbol());
            buffer.writeInt(element.getAtomicNumber());
            buffer.writeInt(element.getColor());
            buffer.writeFloat(element.getElectrodePotential());

            buffer.writeInt(element.getItems().size());
            for (int i = 0; i < element.getItems().size(); i++) {
                buffer.writeUtf(element.getItems().get(i));
                buffer.writeInt(element.getValues().get(i));
            }

            for (int j = 0; j < 10; j++) {
                ElementEquation formula = element.getStats().get(j);
                buffer.writeBoolean(!formula.isEmpty());
                if (!formula.isEmpty()) {
                }
            }
            int size = element.getEnchantments().size();
            buffer.writeInt(size);
            for (int i = 0; i < size; i++) {
                buffer.writeUtf(element.getEnchantments().get(i));
                buffer.writeUtf(element.getEnchantmentTypes().get(i));
                buffer.writeFloat(element.getEnchantmentFactors().get(i));
            }
        }
    }
}
