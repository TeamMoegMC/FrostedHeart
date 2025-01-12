package com.teammoeg.frostedheart.content.town.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.base.team.FHTeamDataManager;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import lombok.Getter;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * 此类用于封装resources，仅提供addUnsafe和costUnsafe两个方法来修改resources，确保在修改资源数量时，occupiedCapacity也会随之修改。
 * 为保证OccupiedCapacity本身不会被意外修改，它并非作为一个service resource储存在resources中，而是以private变量的形式存在这里。
 * 此类中提供的方法都有可能产生不合法的数据(如负数的资源或超过上限的资源)，请使用TownResourceManager来修改资源数量。
 */
public class TownResourceHolder {
    /**
     * The collection that saves all the resources of town.
     * 为了方便城镇访问特定TownResourceKey的资源，将不同种类的资源分开，以减少遍历次数和读取物品tag的次数。
     */
    private Map<ItemStackWrapper, Double> itemResources = new HashMap<>();
    private Map<VirtualResourceKey, Double> virtualResources = new HashMap<>();
    @Getter
    private double occupiedCapacity = 0.0;

    private static final Map<ItemResourceKey, HashSet<ItemStackWrapper>> ITEM_RESOURCE_KEY_CACHE = new HashMap<>();
    private static final Map<ItemStackWrapper, Map<ItemResourceKey, Double>> ITEM_RESOURCE_AMOUNTS = new HashMap<>();
    public static final double DELTA = 1.0/8192;//一个非常小的值，当资源数量小于这个值时，认为资源为0，抵消误差。

    public static final Codec<TownResourceHolder> CODEC = RecordCodecBuilder.create(t -> t.group(
            CodecUtil.defaultValue(CodecUtil.mapCodec("itemStack", ItemStackWrapper.CODEC, "amount", Codec.DOUBLE), new HashMap<>()) .fieldOf("itemResources").forGetter(o->o.itemResources),
            CodecUtil.defaultValue(CodecUtil.mapCodec("virtualKey", VirtualResourceKey.CODEC, "amount", Codec.DOUBLE), new HashMap<>()).fieldOf("virtualResources").forGetter(o->o.virtualResources),
            CodecUtil.defaultValue(Codec.DOUBLE, 0.0).fieldOf("occupiedCapacity").forGetter(o->o.occupiedCapacity)
            ).apply(t, TownResourceHolder::new)
    );

    public TownResourceHolder() {}
    public TownResourceHolder(Map<ItemStackWrapper, Double> itemResources, Map<VirtualResourceKey, Double> virtualResources, double occupiedCapacity) {
        this.itemResources = itemResources;
        this.virtualResources = virtualResources;
        this.occupiedCapacity = occupiedCapacity;
        removeZeros();
        updateCache();
    }

    public TownResourceHolder(Map<ItemStackWrapper, Double> itemResources, Map<VirtualResourceKey, Double> virtualResources) {
        DoubleAdder adder = new DoubleAdder();
        itemResources.values().forEach(adder::add);
        virtualResources.entrySet().stream()
                .filter(entry -> entry.getKey().getType().needCapacity)
                .forEach(entry -> adder.add(entry.getValue()));
        this.itemResources = itemResources;
        this.virtualResources = virtualResources;
        this.occupiedCapacity = adder.doubleValue();
        removeZeros();
        updateCache();
    }

    public static boolean isCached(ItemStack pItemStack){
        ItemStack itemStack = pItemStack.copyWithCount(1);
        AtomicBoolean isCached = new AtomicBoolean(false);
        itemStack.getTags()
                .filter(FHTags.Items.MAP_TAG_TO_TOWN_RESOURCE_KEY::containsKey)
                .findFirst()
                .map(FHTags.Items.MAP_TAG_TO_TOWN_RESOURCE_KEY::get)
                .ifPresent(key -> isCached.set(ITEM_RESOURCE_KEY_CACHE.get(key).contains(new ItemStackWrapper(itemStack))));
        return isCached.get();
    }

    public static double getResourceAmount(ItemStackWrapper itemStackWrapper, ItemResourceKey key){
        if(ITEM_RESOURCE_AMOUNTS.isEmpty()) loadItemResourceAmounts();
        AtomicReference<Double> amount = new AtomicReference<>(0.0);
        if(ITEM_RESOURCE_KEY_CACHE.get(key).contains(itemStackWrapper)){
            amount.set(ITEM_RESOURCE_AMOUNTS.get(itemStackWrapper).getOrDefault(key, 1.0));
        } else{
            itemStackWrapper.getItemStack().getTags()
                    .map(FHTags.Items.MAP_TAG_TO_TOWN_RESOURCE_KEY::get)
                    .filter(key::equals)
                    .findFirst()
                    .ifPresent(key1 -> amount.set(ITEM_RESOURCE_AMOUNTS.get(itemStackWrapper).getOrDefault(key1, 1.0)));
        }
        return amount.get();
    }

    public static double getResourceAmount(ItemStack pItemStack, ItemResourceKey key){
        return getResourceAmount(new ItemStackWrapper(pItemStack), key);
    }

    public static void addItemToCache(ItemStackWrapper itemStackWrapper){
        itemStackWrapper.getItemStack().getTags()
                //仅获取有对应ItemResourceKey的tag
                .filter(FHTags.Items.MAP_TAG_TO_TOWN_RESOURCE_KEY::containsKey)
                //如果已经缓存过，则跳过该物品的tag
                .takeWhile(tag -> !ITEM_RESOURCE_KEY_CACHE.get(ItemResourceKey.fromTagKey(tag)).contains(itemStackWrapper))
                .map(ItemResourceKey::fromTagKey)
                .forEach(key -> ITEM_RESOURCE_KEY_CACHE.computeIfAbsent(key, k -> new HashSet<>()).add(itemStackWrapper));
    }

    public static void addItemToCache(ItemStack pItemStack){
        addItemToCache(new ItemStackWrapper(pItemStack));
    }

    public double get(ItemStackWrapper itemStackWrapper){
        return itemResources.getOrDefault(itemStackWrapper, 0.0);
    }

    public double get(ItemStack pItemStack){
        return get(new ItemStackWrapper(pItemStack));
    }

    public double get(ITownResourceKey key){
        if(key instanceof ItemResourceKey){
            DoubleAdder adder = new DoubleAdder();
            for(ItemStackWrapper itemStackWrapper : ITEM_RESOURCE_KEY_CACHE.getOrDefault((ItemResourceKey)key, new HashSet<>())){
                adder.add(get(itemStackWrapper) * getResourceAmount(itemStackWrapper, (ItemResourceKey)key));
            }
            return adder.doubleValue();
        } else if(key instanceof VirtualResourceKey){
            return virtualResources.getOrDefault(key, 0.0);
        }
        return 0.0;
    }

    public double get(ITownResourceType type){
        double sum = 0.0;
        for(int i = 0; i <= type.getMaxLevel(); i++){
            sum += get(type.generateKey(i));
        }
        return sum;
    }

    /**
     * Get all items stored in town.
     * @return A map that contains all items stored in town.
     */
    public Map<ItemStack, Double> getAllItems(){
        Map<ItemStack, Double> items = new HashMap<>();
        for(ItemStackWrapper itemStackWrapper : itemResources.keySet()){
            if(get(itemStackWrapper) > DELTA){
                items.put(itemStackWrapper.getItemStack(), get(itemStackWrapper));
            }
        }
        return items;
    }

    /**
     * Get all items of the given key stored in town.
     * Can't be used to change the resource.
     * @return A map that contains all items of the given key stored in town.
     */
    public Map<ItemStack, Double> getAllItems(ItemResourceKey key){
        Map<ItemStack, Double> items = new HashMap<>();
        for(ItemStackWrapper itemStackWrapper : ITEM_RESOURCE_KEY_CACHE.get(key)){
            if(get(itemStackWrapper) > DELTA){
                items.put(itemStackWrapper.getItemStack(), get(itemStackWrapper) * getResourceAmount(itemStackWrapper, key));
            }
        }
        return items;
    }

    public Map<VirtualResourceKey, Double> getAllVirtualResources() {
        return Map.copyOf(virtualResources);
    }

    /**
     * Add or subtract resource to the town. Add if amount >= 0, subtract if amount < 0.
     * Only used in this class, use addUnsafe and costUnsafe in this package, use methods in TownResourceManager in other classes.
     */
    private void addSigned(ItemStack pItemStack, double amount){
        if(pItemStack.isEmpty()) return;
        ItemStackWrapper itemStackWrapper = new ItemStackWrapper(pItemStack);
        Double amountExist = itemResources.get(itemStackWrapper);
        if(amountExist == null){
            addItemToCache(itemStackWrapper);
        }
        if(Math.abs(itemResources.merge(itemStackWrapper, amount, Double::sum)) <= DELTA){
            itemResources.remove(itemStackWrapper);
        }
        this.occupiedCapacity += amount;
    }
    private void addSigned(VirtualResourceKey key, double amount){
        virtualResources.merge(key, amount, Double::sum);
        if(key.type.needCapacity){
            this.occupiedCapacity += amount;
        }
    }


    /**
     * Add resource to the town without checking capacity.
     * If you want to change the resource, please use methods in TownResourceManager.
     */
    void addUnsafe(VirtualResourceKey key, double amount){
        if(amount < 0){
            throw new IllegalArgumentException("Amount putted in addUnsafe() must be positive.");
        }
        addSigned(key, amount);
    }

    /**
     * Add resource to the town without checking capacity.
     * If you want to change the resource, please use methods in TownResourceManager.
     * @param ItemStack ItemStack to add. The count of ItemStack will be ignored.
     * @param amount The amount that will actually be added.
     */
    void addUnsafe(ItemStack ItemStack, double amount){
        if(amount < 0) {
            throw new IllegalArgumentException("Amount putted in addUnsafe() must be positive.");
        }
        addSigned(ItemStack, amount);
    }


    /**
     * Add resource to the town without checking capacity.
     * If you want to change the resource, please use methods in TownResourceManager.
     * @param itemStack ItemStack to add. The amount will be added is the count of ItemStack
     */
    void addUnsafe(ItemStack itemStack){
        addUnsafe(itemStack, itemStack.getCount());
    }

    /**
     * Cost resource from the town without checking resource left.
     * If you want to change the resource, please use methods in TownResourceManager.
     */
    void costUnsafe(VirtualResourceKey key, double amount){
        if(amount < 0){
            throw new IllegalArgumentException("Amount putted in costUnsafe() must be positive.");
        }
        addSigned(key, -amount);
    }

    /**
     * Cost resource from the town without checking resource left.
     * If you want to change the resource, please use methods in TownResourceManager.
     * @param itemStack ItemStack to cost. The count of ItemStack will be ignored.
     * @param amount The amount that will actually be cost.
     */
    void costUnsafe(ItemStack itemStack, double amount){
        if(amount < 0){
            throw new IllegalArgumentException("Amount putted in costUnsafe() must be positive.");
        }
        addSigned(itemStack, -amount);
    }

    /**
     * Cost resource from the town without checking resource left.
     * If you want to change the resource, please use methods in TownResourceManager.
     * @param itemStack ItemStack to cost. The amount will be cost is the count of ItemStack
     */
    void costUnsafe(ItemStack itemStack){
        costUnsafe(itemStack, itemStack.getCount());
    }

    /**
     * Set resource.
     */
    void set(VirtualResourceKey key, double amount){
        double different = amount - get(key);
        this.addSigned(key, different);
    }

    /**
     * 遍历存储的所有物品，获取物品对应的ItemResourceKey及其数量，加入到缓存中。
     */
    public void updateCache(){
        for(ItemStackWrapper itemStackWrapper : itemResources.keySet()){
            if(itemStackWrapper.getItemStack().isEmpty()) continue;
            addItemToCache(itemStackWrapper);
        }
    }

    public static void loadItemResourceAmounts(){
        for(ItemResourceAmountRecipe recipe : FHUtils.filterRecipes(FHTeamDataManager.getRecipeManager(), ItemResourceAmountRecipe.TYPE)){
            ItemStackWrapper itemStackWrapper = new ItemStackWrapper(recipe.item);
            if(!ITEM_RESOURCE_AMOUNTS.containsKey(itemStackWrapper)) ITEM_RESOURCE_AMOUNTS.put(itemStackWrapper, new HashMap<>());
            ITEM_RESOURCE_AMOUNTS.get(itemStackWrapper).put(ItemResourceKey.fromTagKey(recipe.resourceTagKey), (double) recipe.amount);
        }
    }

    /**
     * Remove all items and virtual resources with zero amount from map.
     */
    public void removeZeros(){
        itemResources.entrySet().removeIf(entry -> Math.abs(entry.getValue()) <= DELTA || entry.getKey().getItemStack().isEmpty());
        virtualResources.entrySet().removeIf(entry ->Math.abs(entry.getValue()) <= DELTA);
    }

    /**
     * Wrapper for ItemStack, used to store ItemStack in map.
     * The count of ItemStack will be changed to 1 when creating this wrapper.
     */
    @Getter
    public static class ItemStackWrapper {
        public ItemStack itemStack;

        public static final Codec<ItemStackWrapper> CODEC = RecordCodecBuilder.create(t -> t.group(
                        ItemStack.CODEC.fieldOf("itemStack").forGetter(o->o.itemStack)
                ).apply(t, ItemStackWrapper::new)
        );

        public ItemStackWrapper(ItemStack itemStack){
            this.itemStack =itemStack.copyWithCount(1);
        }

        public boolean equals(Object o){
            ItemStack itemStack2;
            if(o instanceof ItemStackWrapper){
                itemStack2 = ((ItemStackWrapper) o).getItemStack();
            } else return false;
            return ItemStack.isSameItemSameTags(itemStack,itemStack2);
        }

        public int hashCode(){
            int itemHash = itemStack.getItem().hashCode();
            int tagHash = itemStack.getTag() == null ? 0 : itemStack.getTag().hashCode();
            return Objects.hash(itemHash,tagHash);
        }

    }

}
