package com.teammoeg.frostedheart.base.item.rankine.alloys;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.teammoeg.frostedheart.base.item.rankine.init.RankineRecipeSerializers;
import com.teammoeg.frostedheart.base.item.rankine.init.RankineRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.stream.Collectors;

public class OldAlloyingRecipe implements Recipe<Container> {

    private final int total;

    private final int tier;
    private final NonNullList<ResourceLocation> elements;
    private final ItemStack recipeOutput;
    private final ResourceLocation id;
    private final NonNullList<Float> mins;
    private final NonNullList<Float> maxes;
    private final NonNullList<Boolean> required;
    private final List<Float> bonusValues;
    private final int color;
    private final List<String> enchantments;
    private final List<String> enchantmentTypes;
    private final int minEnchantability;
    private final int enchantInterval;
    private final int maxEnchantLevelIn;
    private final boolean localName;
    private final boolean forceNBT;

    public OldAlloyingRecipe(ResourceLocation idIn, int totalIn, int tierIn, NonNullList<ResourceLocation> elementsIn, NonNullList<Boolean> requiredIn, NonNullList<Float> minsIn, NonNullList<Float> maxesIn,
                             ItemStack outputIn, List<Float> bonusValuesIn, List<String> enchantmentsIn, List<String> enchantmentTypesIn, int minEnchantabilityIn, int enchantIntervalIn, int maxEnchantLevelIn,
                             boolean nameIn, boolean forceNBTIn, int colorIn) {
        this.id = idIn;
        this.total = totalIn;
        this.required = requiredIn;
        this.tier = tierIn; // Binary: 1 = Alloy Furnace, 2 = Induction Furnace, 3 = Alloy Furnace && Induction Furnace
        this.elements = elementsIn;
        this.recipeOutput = outputIn;
        this.mins = minsIn;
        this.maxes = maxesIn;
        this.bonusValues = bonusValuesIn;
        this.enchantments = enchantmentsIn;
        this.enchantmentTypes = enchantmentTypesIn;
        this.minEnchantability = minEnchantabilityIn;
        this.enchantInterval = enchantIntervalIn;
        this.maxEnchantLevelIn = maxEnchantLevelIn;
        this.color = colorIn;
        this.localName = nameIn;
        this.forceNBT = forceNBTIn;
    }

    public String getGroup() {
        return "";
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.withSize(1,Ingredient.EMPTY);
    }

    public int getTier() {
        return this.tier;
    }

    public List<Ingredient> getIngredientsList(Level worldIn) {
        List<Ingredient> ret = new ArrayList<>();
        for (ResourceLocation rs : this.elements) {
            if (worldIn.getRecipeManager().byKey(rs).isPresent()) {
                ElementRecipe recipe = (ElementRecipe) worldIn.getRecipeManager().byKey(rs).get();
                ret.add(Ingredient.merge(recipe.getIngredients()));
            }
        }
        return ret;
    }

    public List<Ingredient> getIngredientsList(Level worldIn, boolean required) {
        List<Ingredient> ret = new ArrayList<>();
        for (int i = 0; i < this.elements.size(); i++) {
            ResourceLocation rs = this.elements.get(i);
            if (worldIn.getRecipeManager().byKey(rs).isPresent() && this.required.get(i).equals(required)) {
                ElementRecipe recipe = (ElementRecipe) worldIn.getRecipeManager().byKey(rs).get();
                ret.add(Ingredient.merge(recipe.getIngredients()));
            }
        }
        return ret;
    }

    public List<Ingredient> getIngredientGroupList(Level worldIn) {
        List<Integer> nobles = Arrays.asList(2,10,18,36,54,86,118);
        List<Integer> groups = new ArrayList<>();
        for (int i = 0; i < this.elements.size(); i++) {
            ResourceLocation rs = this.elements.get(i);
            if (worldIn.getRecipeManager().byKey(rs).isPresent() && this.required.get(i).equals(false)) {
                ElementRecipe recipe = (ElementRecipe) worldIn.getRecipeManager().byKey(rs).get();
                int atom = recipe.getAtomicNumber();
                int sub = 0;
                if (atom <= 0) {
                    groups.add(0);
                    continue;
                } else if (atom > 118) {
                    groups.add(19);
                    continue;
                }
                for (int x : nobles) {
                    if (x != sub && atom - x > 0) {
                        sub = x;
                    } else {
                        break;
                    }
                }
                int diff = atom - sub;
                groups.add(diff > 18 ? diff - 14 : diff);
            } else {
                groups.add(-1);
            }
        }

        /*
        ORDER MAP BY LIST SIZE (LOWEST -> HIGHEST)
        LOWEST INGREDIENTS PLACED FIRST
        HIGHEST SIZE INGREDIENTS GROUPED TOGETHER AT END OR SPLIT EVENLY ACROSS LIKE MEMBERS, WHICHEVER BEST
         */
        NonNullList<Ingredient> ret = NonNullList.withSize(20,Ingredient.EMPTY);
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i) != -1) {
                ResourceLocation rs = this.elements.get(i);
                if (worldIn.getRecipeManager().byKey(rs).isPresent() ) {
                    ElementRecipe recipe = (ElementRecipe) worldIn.getRecipeManager().byKey(rs).get();
                    ret.set(groups.get(i), Ingredient.merge(Arrays.asList(ret.get(groups.get(i)),Ingredient.merge(recipe.getIngredients()))));
                }
            }

        }
        System.out.println(ret);
        return ret;
    }

    public static <K, V extends List<?>> Map<K, V> sortByListSize(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort((o1, o2) -> o1.getValue().size() > o2.getValue().size() ? 1 : 0);

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public List<Ingredient> getIngredientsGroupedByMinMaxList(Level worldIn) {
        if (this.getIngredientsList(worldIn,true).size() == 0) {
            return Collections.emptyList();
        }
        int size = this.getIngredientsList(worldIn,false).size();
        if (size < 20) {
            return this.getIngredientsList(worldIn, false);
        }
        Map<Float, List<ResourceLocation>> groupToMinMax = new HashMap<>();
        for (int i = this.getIngredientsList(worldIn,true).size(); i < this.elements.size(); i++) {
            ResourceLocation rs = this.elements.get(i);
            Float minMax = this.mins.get(i) - this.maxes.get(i);
            if (groupToMinMax.containsKey(minMax)) {
                List<ResourceLocation> inList = new ArrayList<>(groupToMinMax.get(minMax));
                inList.add(rs);
                groupToMinMax.put(minMax,inList);
            } else {
                groupToMinMax.put(minMax, List.of(rs));
            }
        }

        Map<Float, List<ResourceLocation>> sortedMap = sortByListSize(groupToMinMax);
        NonNullList<Ingredient> ret = NonNullList.withSize(20,Ingredient.EMPTY);

        List<Float> keyList = sortedMap.keySet().stream().toList();
        float curKey;
        float lastKey = 0f;
        int lastKeyIndex = 0;
        int ingredientNumber = 0;
        for (int i = 0; i < sortedMap.size(); i++) {
            curKey = keyList.get(i);
            if (curKey != lastKey) {
                lastKey = curKey;
                lastKeyIndex = ingredientNumber;
            }
            List<ResourceLocation> resourceLocations = sortedMap.get(curKey);

            for (ResourceLocation rs : resourceLocations) {
                Ingredient mergeIng = ret.get(ingredientNumber);
                if (worldIn.getRecipeManager().byKey(rs).isPresent()) {
                    ElementRecipe recipe = (ElementRecipe) worldIn.getRecipeManager().byKey(rs).get();
                    mergeIng = Ingredient.merge(Arrays.asList(mergeIng,Ingredient.merge(recipe.getIngredients())));
                }
                ret.set(ingredientNumber, mergeIng);
                ingredientNumber++;
                if (ingredientNumber > 19) {
                    ingredientNumber = lastKeyIndex;
                }
            }


        }


        return ret;
    }

    public Tuple<Float,Float> getMinMaxByElement(Level levelIn, ItemStack element) {
        ResourceLocation loc = null;
        for (ResourceLocation rs : this.elements) {
            if (levelIn.getRecipeManager().byKey(rs).isPresent()) {
                ElementRecipe recipe = (ElementRecipe) levelIn.getRecipeManager().byKey(rs).get();
                for (Ingredient i : recipe.getIngredients()) {
                    if (i.test(element)) {
                        loc = rs;
                        break;
                    }
                }
            }
        }
        if (loc != null) {
            int index = this.elements.indexOf(loc);
            return new Tuple<>(this.mins.get(index),this.maxes.get(index));
        }
        return new Tuple<>(0f,0f);
    }

    public List<ElementRecipe> getElementList(Level worldIn, boolean required) {
        List<ElementRecipe> ret = new ArrayList<>();
        for (int i = 0; i < this.elements.size(); i++) {
            ResourceLocation rs = this.elements.get(i);
            if (worldIn.getRecipeManager().byKey(rs).isPresent() && this.required.get(i).equals(required)) {
                ElementRecipe recipe = (ElementRecipe) worldIn.getRecipeManager().byKey(rs).get();
                ret.add(recipe);
            }
        }
        return ret;
    }

    public List<ElementRecipe> getElementList(Level worldIn) {
        List<ElementRecipe> ret = new ArrayList<>();
        for (ResourceLocation rs : this.elements) {
            if (worldIn.getRecipeManager().byKey(rs).isPresent()) {
                ElementRecipe recipe = (ElementRecipe) worldIn.getRecipeManager().byKey(rs).get();
                ret.add(recipe);
            }
        }
        return ret;
    }

    public int getTotalRequired() {
        return Collections.frequency(this.required,true);
    }

    public List<Integer> getIndexList(Level worldIn, boolean required) {
        List<Integer> ret = new ArrayList<>();
        for (int i = 0; i < this.elements.size(); i++) {
            ResourceLocation rs = this.elements.get(i);
            if (worldIn.getRecipeManager().byKey(rs).isPresent() && this.required.get(i).equals(required)) {
                ret.add(i);
            }
        }
        return ret;
    }

    public boolean cannotMake(Container inv, Level world) {
        for (Ingredient i : this.getIngredientsList(world,true)) {
            if (!inv.hasAnyOf(Arrays.stream(i.getItems()).map(ItemStack::getItem).collect(Collectors.toSet())))
            {
                return true;
            }
        }
        return false;
    }

    public ItemStack generateResult(Level worldIn, Container inv, int type) {
        if ((getTier() & type) != Math.min(getTier(),type) && getTier() != 0 && type != 0) {
            return ItemStack.EMPTY;
        }

        List<ElementRecipe> currentElements = new ArrayList<>();
        List<Integer> currentMaterial = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                boolean flag = false;
                ElementRecipe element = worldIn.getRecipeManager().getRecipeFor(RankineRecipeTypes.ELEMENT.get(), new SimpleContainer(stack), worldIn).orElse(null);
                if (element != null && getElements().contains(element.getId())) {
                    if (!currentElements.contains(element)) {
                        currentElements.add(element);
                        currentMaterial.add(element.getMaterialCount(stack.getItem()) * stack.getCount());
                    } else {
                        currentMaterial.set(currentElements.indexOf(element),currentMaterial.get(currentElements.indexOf(element)) + element.getMaterialCount(stack.getItem()) * stack.getCount());
                    }
                    flag = true;
                }
                if (!flag) {
                    return ItemStack.EMPTY;
                }
            }
        }
        List<ElementRecipe> req = getElementList(worldIn, true);
        for (ElementRecipe element : req) {
            if (!currentElements.contains(element)) {
                return ItemStack.EMPTY;
            }
        }

        int sum = currentMaterial.stream().mapToInt(Integer::intValue).sum();

        if (currentElements.size() > 1 && (Math.floorDiv(sum,9) > 64 || Math.floorDiv(sum,9) < 1) && currentElements.size() >= getTotalRequired()){
            //System.out.println("Required total " + this.required + " not present or material total not between 1 and 64!");
            return ItemStack.EMPTY;
        }

        List<Integer> percents = new ArrayList<>();
        List<String> symbols = new ArrayList<>();
        for (int j = 0; j < currentElements.size(); j++) {
            ElementRecipe curEl = currentElements.get(j);
            int curPer = Math.round(currentMaterial.get(j) * 100f/sum);
            int windex = getElements().indexOf(curEl.getId());
            if (Math.round(getMins().get(windex) * 100) > curPer || Math.round(getMaxes().get(windex) * 100) < curPer) {
                //System.out.println("Element " + curEl + " does not fall between max or min!");
                //System.out.println("Min: " + Math.round(getMins().get(windex) * 100) + "%");
                //System.out.println("Max: " + Math.round(getMaxes().get(windex) * 100) + "%");
                //System.out.println("Element %: " + curPer + "%");
                return ItemStack.EMPTY;
            }
            symbols.add(curEl.getSymbol());
            percents.add(curPer);
        }
        if (percents.stream().mapToInt(Integer::intValue).sum() != 100 || percents.contains(0)) {
            return ItemStack.EMPTY;
        }
        ItemStack out = new ItemStack(this.recipeOutput.copy().getItem(),Math.floorDiv(sum,9));
        if (out.getItem() instanceof IAlloyItem || this.forceNBT) {
            ((IAlloyItem) out.getItem()).createAlloyNBT(out, worldIn, AlloyRecipeHelper.getDirectComposition(percents,symbols), this.id, !this.getLocalName().isEmpty() ? this.getLocalName() : null);
            if (this.getColor() != 16777215) {
                out.getOrCreateTag().putInt("color",this.getColor());
            }
        }

        return out;
    }

    public String generateRandomResult(Level worldIn) {
        List<Integer> percents = new ArrayList<>();
        List<String> symbols = new ArrayList<>();
        RandomSource rand = worldIn.getRandom();
        List<ElementRecipe> req = getElementList(worldIn, true);
        List<ElementRecipe> nonreq = getElementList(worldIn, false);
        if (req.isEmpty() && nonreq.isEmpty()) {
            return "80Hg-20Au";
        }
        int limit = Math.min(5,req.size() + nonreq.size());
        int size = req.size();
        for (int i = 0; i < limit - size; i++) {
            List<ElementRecipe> available = nonreq.stream().filter(o -> !req.contains(o)).collect(Collectors.toList());
            req.add(available.get(rand.nextInt(available.size())));
        }
       // System.out.println("Selected elements: " + req);
        List<Integer> maxes = new ArrayList<>();
        List<Integer> mins = new ArrayList<>();
        for (ElementRecipe element : req) {
            int windex = getElements().indexOf(element.getId());
            symbols.add(element.getSymbol());
            maxes.add(Math.round(getMaxes().get(windex) * 100));
            mins.add(Math.round(getMins().get(windex) * 100));
        }
        //System.out.println("Potential maxes: " + maxes);
        //System.out.println("Potential mins: " + mins);
        List<String> options = new ArrayList<>();
        for (int b = 0; b < Math.pow(2,limit); b++) {
            List<Integer> sum = new ArrayList<>();
            String s = String.format("%"+ limit +"s",Integer.toBinaryString(b)).replace(' ', '0');
            for (int i = 0; i < s.length(); i++) {
                int temp = Integer.parseInt(String.valueOf(s.charAt(i)));
                if (temp == 0) {
                    sum.add(maxes.get(i));
                } else {
                    sum.add(mins.get(i));
                }
            }
            int summation = sum.stream().reduce(0, Integer::sum);
            if (summation == 100) {
                options.add(s);
            }
        }

        //System.out.println("OPTIONS: " + options);
        if (options.size() != 0) {
            String selectedOption = options.get(rand.nextInt(options.size()));
            for (int i = 0; i < selectedOption.length(); i++) {
                int temp = Integer.parseInt(String.valueOf(selectedOption.charAt(i)));
                if (temp == 0) {
                    percents.add(maxes.get(i));
                } else {
                    percents.add(mins.get(i));
                }
            }
            return AlloyRecipeHelper.getDirectComposition(percents,symbols);
        } else
        {
            return "80Hg-20Au";
        }




    }

    public List<String> getEnchantments() {
        return enchantments;
    }

    public List<String> getEnchantmentTypes() {
        return enchantmentTypes;
    }

    public NonNullList<Float> getMins() {
        return mins;
    }

    public NonNullList<Float> getMaxes() {
        return maxes;
    }

    public List<Float> getBonusValues() {
        return bonusValues;
    }

    public NonNullList<Boolean> getRequired() {
        return required;
    }

    public String getLocalName() {
        if (!this.localName) {
            return "";
        } else {
            String[] s = this.id.getPath().split("/");
            return "item." + this.id.getNamespace() + "." + s[s.length-1];
        }
    }


    public int getColor() {
        return color;
    }

    public int getBonusDurability() { return Math.round(this.getBonusValues().get(0));}

    public float getBonusMiningSpeed() { return this.getBonusValues().get(1);}

    public int getBonusMiningLevel() { return Math.round(this.getBonusValues().get(2));}

    public int getBonusEnchantability() { return Math.round(this.getBonusValues().get(3));}

    public float getBonusDamage() { return this.getBonusValues().get(4);}

    public float getBonusAttackSpeed() { return this.getBonusValues().get(5);}

    public float getBonusCorrosionResistance() { return this.getBonusValues().get(6);}

    public float getBonusHeatResistance() { return this.getBonusValues().get(7);}

    public float getBonusKnockbackResistance() { return this.getBonusValues().get(8);}

    public float getBonusToughness() { return this.getBonusValues().get(9);}

    public int getMinEnchantability() {
        return minEnchantability;
    }

    public int getEnchantInterval() {
        return enchantInterval;
    }

    public int getMaxEnchantLevelIn() {
        return maxEnchantLevelIn;
    }

    public float getBonusStat(int stat) {
        switch (stat) {
            case 0:
                return this.getBonusDurability();
            case 1:
                return this.getBonusMiningSpeed();
            case 2:
                return this.getBonusMiningLevel();
            case 3:
                return this.getBonusEnchantability();
            case 4:
                return this.getBonusDamage();
            case 5:
                return this.getBonusAttackSpeed();
            case 6:
                return this.getBonusCorrosionResistance();
            case 7:
                return this.getBonusHeatResistance();
            case 8:
                return this.getBonusKnockbackResistance();
            case 9:
                return this.getBonusToughness();
        }
        return -1;
    }

    @Override
    public boolean matches(Container inv, Level worldIn) {
        //TODO
//        if (inv instanceof AlloyFurnaceTile) {
//            return !cannotMake(inv,worldIn) && !generateResult(worldIn,inv,1).isEmpty();
//        } else if (inv instanceof InductionFurnaceTile) {
//            return !cannotMake(inv,worldIn) && !generateResult(worldIn, inv,2).isEmpty();
//        } else if (getTier() != 0){
//            return !cannotMake(inv,worldIn) && !generateResult(worldIn, inv,3).isEmpty();
//        } else {
//            return false;
//        }
        return false;
    }

    @Override
    public ItemStack assemble(Container inv, RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        ItemStack out = this.recipeOutput.copy();
        if (this.getColor() != 16777215) {
            out.getOrCreateTag().putInt("color",this.getColor());
        }

        if (!this.getLocalName().isEmpty()) {
            out.getOrCreateTag().putString("nameOverride",this.getLocalName());
        }
        return out;
    }

    public int getTotal() {
        return this.total;
    }

    public NonNullList<ResourceLocation> getElements() {
        return this.elements;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RankineRecipeSerializers.ALLOYING_RECIPE_SERIALIZER.get();
    }

    public static ItemStack deserializeItem(JsonObject object) {
        String s = GsonHelper.getAsString(object, "item");
        Item item = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(s)).orElseThrow(() -> {
            return new JsonSyntaxException("Unknown item '" + s + "'");
        });

        if (object.has("data")) {
            throw new JsonParseException("Disallowed data tag found");
        } else {
            int i = GsonHelper.getAsInt(object, "count", 1);
            return AlloyIngredientHelper.getItemStack(object, true, false);
        }
    }

    @Override
    public RecipeType<?> getType() {
        return RankineRecipeTypes.ALLOYING.get();
    }

    public static class Serializer implements RecipeSerializer<OldAlloyingRecipe> {
        private static final ResourceLocation NAME = new ResourceLocation("rankine", "alloying");
        public OldAlloyingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            int reqcount = 0;
            int t = json.get("total").getAsInt();
            int y;
            if (json.has("tier")) {
                y = json.get("tier").getAsInt();
            } else {
                y = 3;
            }
            int c;
            if (json.has("color")) {
                c = Math.max(0,json.get("color").getAsInt());
            } else {
                c = 16777215;
            }
            boolean n = true;
            if (json.has("genName")) {
                n = json.get("genName").getAsBoolean();
            }
            boolean force = json.has("forceNBT") && json.get("forceNBT").getAsBoolean();

            String s1 = GsonHelper.getAsString(json, "result");
            ResourceLocation resourcelocation = new ResourceLocation(s1);
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getOptional(resourcelocation).orElseThrow(() -> new IllegalStateException("Item: " + s1 + " does not exist")));

            NonNullList<ResourceLocation> elements = NonNullList.withSize(t, new ResourceLocation(""));
            NonNullList<Float> mins = NonNullList.withSize(t, 0f);
            NonNullList<Float> maxes = NonNullList.withSize(t, 0f);
            NonNullList<Boolean> reqs = NonNullList.withSize(t, false);

            for (int i = 0; i < t; i++) {
                String input = "input" + (i+1);
                if (json.has(input)) {
                    JsonObject object = GsonHelper.getAsJsonObject(json, input);
                    if (object.has("element")){
                        elements.set(i, new ResourceLocation(object.get("element").getAsString()));
                    } else {
                        throw new JsonParseException("Object 'element' for " + input + " does not exist!");
                    }


                    if (object.has("min")){
                        mins.set(i,Math.min(Math.max(object.get("min").getAsFloat(),0f),1f));
                    }

                    if (object.has("max")){
                        maxes.set(i,Math.min(Math.max(object.get("max").getAsFloat(),0f),1f));
                    }

                    if (object.has("required")){
                        reqs.set(i,object.get("required").getAsBoolean());
                    }

                }
            }

            int r = Collections.frequency(reqs,true);
            if (r > 6) {
                throw new JsonParseException("Unsupported number of alloy ingredient requirements (" + r + ") in " + json);
            }

            String[] stats = new String[]{"durability","miningspeed","mininglevel","enchantability","damage","attackspeed",
                    "corrosionresist","heatresist","knockbackresist","toughness"};
            List<Float> bonusStats = new ArrayList<>();
            for (String stat : stats) {
                if (json.has(stat)) {
                    bonusStats.add(GsonHelper.getAsFloat(json, stat));
                } else {
                    bonusStats.add(0f);
                }
            }

            List<String> enchantments = new ArrayList<>();
            List<String> enchantmentTypes = new ArrayList<>();
            if (json.has("enchantments")) {
                JsonArray e = GsonHelper.getAsJsonArray(json,"enchantments");
                JsonArray eTypes = GsonHelper.getAsJsonArray(json,"enchantmentTypes");
                for (int i = 0; i < e.size(); i++) {
                    enchantments.add(e.get(i).getAsString().toLowerCase(Locale.ROOT));
                    enchantmentTypes.add(eTypes.get(i).getAsString().toUpperCase(Locale.ROOT));
                }
            }

            int startEnchant = json.has("minEnchantability") ? json.get("minEnchantability").getAsInt() : 20;
            int interval = json.has("enchantInterval") ? json.get("enchantInterval").getAsInt() : 5;
            int maxLvl = json.has("maxEnchantLevel") ? json.get("maxEnchantLevel").getAsInt() : 3;
            return new OldAlloyingRecipe(recipeId, t, y, elements, reqs, mins, maxes, stack, bonusStats,enchantments,enchantmentTypes,startEnchant,interval,maxLvl,n,force,c);
        }

        public OldAlloyingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            int t = buffer.readInt();
            int y = buffer.readInt();
            List<Float> bonusStats = new ArrayList<>();

            NonNullList<ResourceLocation> elements = NonNullList.withSize(t, new ResourceLocation(""));

            for(int k = 0; k < elements.size(); ++k) {
                elements.set(k, new ResourceLocation(buffer.readUtf()));
            }

            ItemStack stack = buffer.readItem();

            NonNullList<Float> mins = NonNullList.withSize(t, 0f);
            for(int k = 0; k < mins.size(); ++k) {
                mins.set(k, buffer.readFloat());
            }


            NonNullList<Float> maxes = NonNullList.withSize(t,0f);
            for(int k = 0; k < maxes.size(); ++k) {
                maxes.set(k, buffer.readFloat());
            }

            NonNullList<Boolean> reqs = NonNullList.withSize(t,false);
            for(int k = 0; k < reqs.size(); ++k) {
                reqs.set(k, buffer.readBoolean());
            }

            for (int k = 0; k < 10; k++) {
                bonusStats.add(buffer.readFloat());
            }

            boolean n = buffer.readBoolean();
            boolean force = buffer.readBoolean();
            int c = buffer.readInt();

            int size = buffer.readInt();
            List<String> enchantments = new ArrayList<>();
            List<String> enchantmentTypes = new ArrayList<>();
            for (int j = 0; j < size; j++) {
                enchantments.add(buffer.readUtf().toLowerCase(Locale.ROOT));
                enchantmentTypes.add(buffer.readUtf().toUpperCase(Locale.ROOT));
            }

            int startEnchant = buffer.readInt();
            int interval = buffer.readInt();
            int maxLvl = buffer.readInt();
            return new OldAlloyingRecipe(recipeId,t,y, elements, reqs, mins, maxes, stack,bonusStats,enchantments,enchantmentTypes,startEnchant,interval,maxLvl,n,force,c);
        }

        public void toNetwork(FriendlyByteBuf buffer, OldAlloyingRecipe recipe) {
            buffer.writeInt(recipe.total);
            buffer.writeInt(recipe.tier);

            int count = 0;
            for(ResourceLocation element : recipe.elements) {
                buffer.writeUtf(element.toString());
                count++;
            }
            while (count < recipe.total) {
                buffer.writeUtf("rankine:elements/mercury");
                count++;
            }

            buffer.writeItem(recipe.recipeOutput);

            count = 0;
            for (float chance : recipe.mins) {
                buffer.writeFloat(chance);
                count++;
            }
            while (count < recipe.total) {
                buffer.writeFloat(0f);
                count++;
            }

            count = 0;
            for (float add : recipe.maxes) {
                buffer.writeFloat(add);
                count++;
            }
            while (count < recipe.total) {
                buffer.writeFloat(0f);
                count++;
            }

            count = 0;
            for (boolean add : recipe.required) {
                buffer.writeBoolean(add);
                count++;
            }
            while (count < recipe.total) {
                buffer.writeBoolean(false);
                count++;
            }

            for (int k = 0; k < 10; k++) {
                buffer.writeFloat(recipe.getBonusValues().get(k));
            }

            buffer.writeBoolean(recipe.localName);
            buffer.writeBoolean(recipe.forceNBT);
            buffer.writeInt(recipe.getColor());

            int size = recipe.getEnchantments().size();
            buffer.writeInt(size);
            for (int i = 0; i < size; i++) {
                buffer.writeUtf(recipe.getEnchantments().get(i));
                buffer.writeUtf(recipe.getEnchantmentTypes().get(i));
            }

            buffer.writeInt(recipe.minEnchantability);
            buffer.writeInt(recipe.enchantInterval);
            buffer.writeInt(recipe.maxEnchantLevelIn);
        }
    }

}
