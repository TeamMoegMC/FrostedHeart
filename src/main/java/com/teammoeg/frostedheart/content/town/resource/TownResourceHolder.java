/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.town.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.chorda.util.CDistHelper;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;

import lombok.Getter;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * 此类用于封装resources，仅提供addUnsafe和costUnsafe两个方法来修改resources，确保在修改资源数量时，occupiedCapacity也会随之修改。
 * 为保证OccupiedCapacity本身不会被意外修改，它并非作为一个service resource储存在resources中，而是以private变量的形式存在这里。
 * 此类中提供的方法都有可能产生不应出现的数据(如负数的资源或超过上限的资源)，请使用TownResourceManager来修改资源数量。
 */
public class TownResourceHolder {
    /**
     * The collection that saves all the resources of town.
     * ItemStack没有实现equals和hashCode方法，使用ItemStackWrapper进行封装来代替。
     */
    private Map<ItemStackWrapper, Double> itemResources = new HashMap<>();
    private Map<VirtualResourceKey, Double> virtualResources = new HashMap<>();
    /**
     * 表示已占用的资源容量。
     * 使用此类的中的addUnsafe和costUnsafe方法修改需要占用容量的资源时，已占用资源会随之修改。
     * 对于物品，每一个物品都占用1个容量。每一个需要占用容量的虚拟资源也占用1个容量。
     */
    @Getter
    private double occupiedCapacity = 0.0;

    /**
     * 缓存ItemResourceKey对应的物品。
     * 存储在城镇中的物品，在读取数据，即创建新TownResourceHolder实例时，都会顺带加入缓存中。
     * 若城镇中没有储存过改物品，物品使用AddUnsafe方法加入城镇时，也会存入缓存中。
     * 因此，这个缓存理论上涵盖了所有存储在城镇中的物品，按照某个TownResourceKey消耗资源时，不需要考虑没有存入缓存的物品而去遍历所有物品。
     */
    private static final Map<ItemResourceKey, HashSet<ItemStackWrapper>> ITEM_RESOURCE_KEY_CACHE = new HashMap<>();
    private static final Map<ItemStackWrapper, Map<ItemResourceKey, Double>> ITEM_RESOURCE_AMOUNTS = new HashMap<>();

    public static final double DELTA = 1.0/8192;//一个非常小的值，当资源数量小于这个值时，认为资源为0，抵消误差。

    public static final Codec<TownResourceHolder> CODEC = RecordCodecBuilder.create(t -> t.group(
            CodecUtil.mapCodec("itemStack", ItemStackWrapper.CODEC, "amount", Codec.DOUBLE) .optionalFieldOf("itemResources",Map.of()).forGetter(o->o.itemResources),
            CodecUtil.mapCodec("virtualKey", VirtualResourceKey.CODEC, "amount", Codec.DOUBLE).optionalFieldOf("virtualResources",Map.of()).forGetter(o->o.virtualResources),
            Codec.DOUBLE.optionalFieldOf("occupiedCapacity",0d).forGetter(o->o.occupiedCapacity)
            ).apply(t, TownResourceHolder::new)
    );

    public TownResourceHolder() {}
    public TownResourceHolder(Map<ItemStackWrapper, Double> itemResources, Map<VirtualResourceKey, Double> virtualResources, double occupiedCapacity) {
        this.itemResources.putAll(itemResources);
        this.virtualResources.putAll(virtualResources);
        this.occupiedCapacity = occupiedCapacity;
        removeZeros();
        updateCache();
    }

    //不输入已占用容量，自行计算。
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

    /**
     * 判断是否缓存了该物品。
     */
    public static boolean isCached(ItemStack pItemStack){
        ItemStack itemStack = pItemStack.copyWithCount(1);
        AtomicBoolean isCached = new AtomicBoolean(false);
        itemStack.getTags()
                .filter(FHTags.Items.MAP_TAG_TO_TOWN_RESOURCE_KEY::containsKey)
                .findFirst()
                .map(FHTags.Items.MAP_TAG_TO_TOWN_RESOURCE_KEY::get)
                .ifPresent(key -> {
                    HashSet<ItemStackWrapper> itemOfKey = ITEM_RESOURCE_KEY_CACHE.get(key);
                    if(itemOfKey == null || itemOfKey.isEmpty()){
                        isCached.set(false);
                        return;
                    }
                    isCached.set(ITEM_RESOURCE_KEY_CACHE.get(key).contains(new ItemStackWrapper(itemStack)));
                });
        return isCached.get();
    }

    /**
     * 获取单个物品对应的的某资源数量。
     * @param itemStackWrapper 物品（包装类）
     * @param key 物品要转化为的ItemResourceKey
     * @return 单个该物品能转化为输入的ItemResourceKey的数量
     */
    public static double getResourceAmount(ItemStackWrapper itemStackWrapper, ItemResourceKey key){
        if(ITEM_RESOURCE_AMOUNTS.isEmpty()) loadItemResourceAmounts();
        AtomicReference<Double> amount = new AtomicReference<>(0.0);
        HashSet<ItemStackWrapper> itemsOfKey = ITEM_RESOURCE_KEY_CACHE.get(key);
        if(itemsOfKey == null || itemsOfKey.isEmpty()) return 0.0;
        if(itemsOfKey.contains(itemStackWrapper)){
            Map<ItemResourceKey, Double> itemAmounts = ITEM_RESOURCE_AMOUNTS.get(itemStackWrapper);
            if(itemAmounts == null) return 1.0;
            amount.set(itemAmounts.getOrDefault(key, 1.0));
        } else{
            itemStackWrapper.getItemStack().getTags()
                    .map(FHTags.Items.MAP_TAG_TO_TOWN_RESOURCE_KEY::get)
                    .filter(key::equals)
                    .findFirst()
                    .ifPresent(key1 -> {
                        Map<ItemResourceKey, Double> itemAmounts = ITEM_RESOURCE_AMOUNTS.get(itemStackWrapper);
                        if(itemAmounts == null || itemAmounts.isEmpty()) {
                            amount.set(1.0);
                            return;
                        }
                        amount.set(itemAmounts.getOrDefault(key1, 1.0));
                    });
        }
        return amount.get();
    }

    /**
     * 获取单个物品对应的的某资源数量。
     * @param pItemStack 物品
     * @param key 物品要转化为的ItemResourceKey
     * @return 单个该物品能转化为输入的ItemResourceKey的数量
     */
    public static double getResourceAmount(ItemStack pItemStack, ItemResourceKey key){
        return getResourceAmount(new ItemStackWrapper(pItemStack), key);
    }

    /**
     * 获取物品的所有ItemResourceKey，并加入到缓存中。
     * 若加入过程中发现该物品已经缓存过，则直接跳过后续内容。
     * @param itemStackWrapper ItemStack的包装类
     */
    public static void addItemToCache(ItemStackWrapper itemStackWrapper){
        itemStackWrapper.getItemStack().getTags()
                //仅获取有对应ItemResourceKey的tag
                .filter(FHTags.Items.MAP_TAG_TO_TOWN_RESOURCE_KEY::containsKey)
                //转化为ItemResourceKey
                .map(ItemResourceKey::fromTagKey)
                //如果已经缓存过，则跳过该物品的tag
                .takeWhile(key -> !ITEM_RESOURCE_KEY_CACHE.computeIfAbsent(key, k -> new HashSet<>()).contains(itemStackWrapper))
                .forEach(key -> ITEM_RESOURCE_KEY_CACHE.computeIfAbsent(key, k -> new HashSet<>()).add(itemStackWrapper));
    }

    /**
     * 获取物品的所有ItemResourceKey，并加入到缓存中。
     * 若加入过程中发现该物品已经缓存过，则直接跳过后续内容。
     * @param pItemStack 物品
     */
    public static void addItemToCache(ItemStack pItemStack){
        addItemToCache(new ItemStackWrapper(pItemStack));
    }

    /**
     * 获取城镇中某个物品的储量。
     */
    public double get(ItemStackWrapper itemStackWrapper){
        return itemResources.getOrDefault(itemStackWrapper, 0.0);
    }

    /**
     * 获取城镇中某个物品的储量。
     */
    public double get(ItemStack pItemStack){
        return get(new ItemStackWrapper(pItemStack));
    }

    /**
     * 获取城镇中某个ITownResourceKey的储量。

     * 若为ItemResourceKey，获取的是城镇中，所有该ItemResourceKey对应物品的(物品数量 * 单个物品能转化为ItemResourceKey数量)之和。
     * 这也是TownResourceManager中，两种cost(ItemResourceKey)能消耗的最大数量。
     * 举个例子，比如城镇中，有20个铁锭、10个铁块，1个铁锭可转化为1 metal_1，1个铁块可转化为9 metal_1，使用这个get方法获取metal_1的数量，会得到20*1+10*9=110.
     */
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
     * Only used in this class.
     * Use addUnsafe and costUnsafe in this package.
     * Use methods in TownResourceManager in other classes.
     */
    private void addSigned(ItemStack pItemStack, double amount){
        if(pItemStack.isEmpty()) return;
        ItemStackWrapper itemStackWrapper = new ItemStackWrapper(pItemStack);
        Double amountExist = itemResources.get(itemStackWrapper);
        if(amountExist == null || amountExist <= DELTA){
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

    /**
     * 读取所欲城镇相关的物品对应资源数量的recipe，并缓存起来。
     */
    public static void loadItemResourceAmounts(){
        for(ItemResourceAmountRecipe recipe : CUtils.filterRecipes(CDistHelper.getRecipeManager(), ItemResourceAmountRecipe.TYPE)){
            ItemStackWrapper itemStackWrapper = new ItemStackWrapper(recipe.item);
            if(!ITEM_RESOURCE_AMOUNTS.containsKey(itemStackWrapper)) ITEM_RESOURCE_AMOUNTS.put(itemStackWrapper, new HashMap<>());
            ITEM_RESOURCE_AMOUNTS.computeIfAbsent(itemStackWrapper, k -> new HashMap<>()).put(ItemResourceKey.fromTagKey(recipe.resourceTagKey), (double) recipe.amount);
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
     * Wrapper for ItemStack, added special hashCode and equals method, for saving ItemStack in HashMap.
     * The count of ItemStack will be changed to 1 when creating this wrapper. Because TownResourceHolder used other things to save the amount of items.
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
