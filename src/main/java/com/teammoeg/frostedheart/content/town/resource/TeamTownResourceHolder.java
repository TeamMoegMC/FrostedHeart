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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableDouble;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.util.CDistHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;

import lombok.Getter;
import net.minecraft.world.item.ItemStack;

/**
 * 此类用于封装resources，仅提供addUnsafe和costUnsafe两个方法来修改resources，确保在修改资源数量时，occupiedCapacity也会随之修改。
 * 为保证OccupiedCapacity本身不会被意外修改，它并非作为一个service resource储存在resources中，而是以private变量的形式存在这里。
 * 此类中提供的方法都有可能产生不应出现的数据(如负数的资源或超过上限的资源)，请使用TownResourceManager来修改资源数量。
 */
public class TeamTownResourceHolder {
    /**
     * The collection that saves all the resources of town.
     */
    //private Map<ItemStackResourceKey, Double> itemResources = new HashMap<>();
    //private Map<VirtualResourceAttribute, Double> virtualResources = new HashMap<>();
    private final Map<ITownResourceKey, Double> resources = new HashMap<>();

    public final TeamTownResourceActionExecutorHandler actionExecutor = new TeamTownResourceActionExecutorHandler(this);

    /**
     * 表示已占用的资源容量。
     * 使用此类的中的addUnsafe和costUnsafe方法修改需要占用容量的资源时，已占用资源会随之修改。
     * 对于物品，每一个物品都占用1个容量。每一个需要占用容量的虚拟资源也占用1个容量。
     */
    @Getter
    private double occupiedCapacity = 0.0;

    /**
     * 缓存ItemResourceAttribute对应的物品。
     * 存储在城镇中的物品，在读取数据，即创建新TownResourceHolder实例时，都会顺带加入缓存中。
     * 若城镇中没有储存过改物品，物品使用AddUnsafe方法加入城镇时，也会存入缓存中。
     * 因此，这个缓存理论上涵盖了所有存储在城镇中的物品，按照某个TownResourceAttribute消耗资源时，不需要考虑没有存入缓存的物品而去遍历所有物品。
     */
    private static final Map<ItemResourceAttribute, HashSet<ItemStackResourceKey>> ITEM_RESOURCE_ATTRIBUTE_CACHE = new HashMap<>();
    private static final Map<ItemStackResourceKey, Map<ItemResourceAttribute, Double>> ITEM_RESOURCE_AMOUNTS = new HashMap<>();

    public static final double DELTA = 1.0/8192;//一个非常小的值，当资源数量小于这个值时，认为资源为0，抵消误差。

    public static final Codec<TeamTownResourceHolder> CODEC = RecordCodecBuilder.create(t -> t.group(
            //CodecUtil.mapCodec("itemStack", ItemStackResourceKey.CODEC, "amount", Codec.DOUBLE) .optionalFieldOf("itemResources",Map.of()).forGetter(o->o.itemResources),
            //CodecUtil.mapCodec("virtualKey", VirtualResourceAttribute.CODEC, "amount", Codec.DOUBLE).optionalFieldOf("virtualResources",Map.of()).forGetter(o->o.virtualResources),
            CodecUtil.mapCodec("resourceKey", ITownResourceKey.CODEC, "amount", Codec.DOUBLE).optionalFieldOf("resources",Map.of()).forGetter(o->o.resources),
            Codec.DOUBLE.optionalFieldOf("occupiedCapacity",0d).forGetter(o->o.occupiedCapacity)
            ).apply(t, TeamTownResourceHolder::new)
    );

    public TeamTownResourceHolder() {}
    public TeamTownResourceHolder(Map<ITownResourceKey, Double> resources, double occupiedCapacity) {
        this.resources.putAll(resources);
        this.occupiedCapacity = occupiedCapacity;
        removeZeros();
        updateCache();
    }

    //不输入已占用容量，自行计算。
    public TeamTownResourceHolder(Map<ITownResourceKey, Double> resources) {
        double adder = 0.0;
        for(Map.Entry<ITownResourceKey, Double> entry : resources.entrySet()){
            ITownResourceKey key = entry.getKey();
            resources.put(key, entry.getValue());
            if(key instanceof ItemStackResourceKey){
                adder += entry.getValue();
            } else if(key instanceof VirtualResourceAttribute virtualKey){
                adder += virtualKey.getType().needCapacity? entry.getValue() : 0;
            }
        }
        this.occupiedCapacity = adder;
        removeZeros();
        updateCache();
    }

    /**
     * 判断是否缓存了该物品。
     */
    public static boolean isCached(ItemStack pItemStack){
        ItemStack itemStack = pItemStack.copyWithCount(1);
        MutableBoolean isCached = new MutableBoolean(false);
        itemStack.getTags()
                .filter(FHTags.Items.MAP_TAG_TO_TOWN_RESOURCE_ATTRIBUTE::containsKey)
                .findFirst()
                .map(FHTags.Items.MAP_TAG_TO_TOWN_RESOURCE_ATTRIBUTE::get)
                .ifPresent(attribute -> {
                    HashSet<ItemStackResourceKey> itemOfAttribute = ITEM_RESOURCE_ATTRIBUTE_CACHE.get(attribute);
                    if(itemOfAttribute == null || itemOfAttribute.isEmpty()){
                        isCached.setValue(false);
                        return;
                    }
                    isCached.setValue(ITEM_RESOURCE_ATTRIBUTE_CACHE.get(attribute).contains(new ItemStackResourceKey(itemStack)));
                });
        return isCached.getValue();
    }

    /**
     * <b>获取【单个物品对应的的某资源数量】!!!</b>
     * @param itemStackResourceKey 物品（包装类）
     * @param attribute 物品要转化为的ItemResourceAttribute
     * @return 单个该物品能转化为输入的ItemResourceAttribute的数量
     */
    public static double getResourceAmount(ItemStackResourceKey itemStackResourceKey, ItemResourceAttribute attribute){
        if(ITEM_RESOURCE_AMOUNTS.isEmpty()) loadItemResourceAmounts();
        MutableDouble amount = new MutableDouble(0.0);
        HashSet<ItemStackResourceKey> itemsOfAttribute = ITEM_RESOURCE_ATTRIBUTE_CACHE.get(attribute);
        if(itemsOfAttribute == null || itemsOfAttribute.isEmpty()) return 0.0;
        if(itemsOfAttribute.contains(itemStackResourceKey)){
            Map<ItemResourceAttribute, Double> itemAmounts = ITEM_RESOURCE_AMOUNTS.get(itemStackResourceKey);
            if(itemAmounts == null) return 1.0;
            amount.setValue(itemAmounts.getOrDefault(attribute, 1.0));
        } else{
            itemStackResourceKey.getItemStack().getTags()
                    .map(FHTags.Items.MAP_TAG_TO_TOWN_RESOURCE_ATTRIBUTE::get)
                    .filter(attribute::equals)
                    .findFirst()
                    .ifPresent(attribute1 -> {
                        Map<ItemResourceAttribute, Double> itemAmounts = ITEM_RESOURCE_AMOUNTS.get(itemStackResourceKey);
                        if(itemAmounts == null || itemAmounts.isEmpty()) {
                            amount.setValue(1.0);
                            return;
                        }
                        amount.setValue(itemAmounts.getOrDefault(attribute1, 1.0));
                    });
        }
        return amount.getValue();
    }

    /**
     * 获取单个物品对应的的某资源数量。
     * @param pItemStack 物品
     * @param attribute 物品要转化为的ItemResourceAttribute
     * @return 单个该物品能转化为输入的ItemResourceAttribute的数量
     */
    public static double getResourceAmount(ItemStack pItemStack, ItemResourceAttribute attribute){
        return getResourceAmount(new ItemStackResourceKey(pItemStack), attribute);
    }

    /**
     * 获取物品的所有ItemResourceAttribute，并加入到缓存中。
     * 若加入过程中发现该物品已经缓存过，则直接跳过后续内容。
     * @param itemStackResourceKey ItemStack的包装类
     */
    public static void addItemToCache(ItemStackResourceKey itemStackResourceKey){
        itemStackResourceKey.getItemStack().getTags()
                //仅获取有对应ItemResourceAttribute的tag
                .filter(FHTags.Items.MAP_TAG_TO_TOWN_RESOURCE_ATTRIBUTE::containsKey)
                //转化为ItemResourceAttribute
                .map(ItemResourceAttribute::fromTagKey)
                //如果已经缓存过，则跳过该物品的tag
                .takeWhile(attribute -> !ITEM_RESOURCE_ATTRIBUTE_CACHE.computeIfAbsent(attribute, k -> new HashSet<>()).contains(itemStackResourceKey))
                .forEach(attribute -> ITEM_RESOURCE_ATTRIBUTE_CACHE.computeIfAbsent(attribute, k -> new HashSet<>()).add(itemStackResourceKey));
    }

    /**
     * 获取物品的所有ItemResourceAttribute，并加入到缓存中。
     * 若加入过程中发现该物品已经缓存过，则直接跳过后续内容。
     * @param pItemStack 物品
     */
    public static void addItemToCache(ItemStack pItemStack){
        addItemToCache(new ItemStackResourceKey(pItemStack));
    }



    /**
     * 获取城镇中某种或某类资源的数量。
     * 支持获取{@link ITownResourceKey}, {@link ITownResourceAttribute}和{@link ITownResourceType}的数量
     * <p>
     * 当尝试获取Attribute数量时
     * 若为ItemResourceAttribute，获取的是城镇中，所有该ItemResourceAttribute对应物品的(物品数量 * 单个物品能转化为ItemResourceAttribute数量)之和。
     * 这也是TownResourceManager中，两种cost(ItemResourceAttribute)能消耗的最大数量。
     * 举个例子，比如城镇中，有20个铁锭、10个铁块，1个铁锭可转化为1 metal_1，1个铁块可转化为9 metal_1，使用这个get方法获取metal_1的数量，会得到20*1+10*9=110.
     * </p>
     */
    public double get(IGettable thing){
        if(thing instanceof ITownResourceKey townResourceKey){
            return resources.getOrDefault(townResourceKey, 0.0);
        }
        if(thing instanceof ITownResourceAttribute attribute){
            //VirtualResourceAttribute由于同时属于ITownResourceKey，在前面处理掉了
            if(thing instanceof ItemResourceAttribute itemResourceAttribute){
                MutableDouble adder = new MutableDouble();
                for(ItemStackResourceKey itemStackResourceKey : ITEM_RESOURCE_ATTRIBUTE_CACHE.getOrDefault(itemResourceAttribute, new HashSet<>())){
                    adder.add(get(itemStackResourceKey) * getResourceAmount(itemStackResourceKey, itemResourceAttribute));
                }
                return adder.doubleValue();
            }
        }
        if(thing instanceof ITownResourceType type){
            double sum = 0.0;
            for(int i = 0; i <= type.getMaxLevel(); i++){
                sum += get(type.generateAttribute(i));
            }
            return sum;
        }
        return 0.0;
    }

    /**
     * 获取城镇中某个物品的储量。
     */
    public double get(ItemStack pItemStack){
        return get(new ItemStackResourceKey(pItemStack));
    }

    /**
     * Get all items stored in town.
     * @return A map that contains all items stored in town.
     */
    public Map<ItemStack, Double> getAllItems(){
        Map<ItemStack, Double> items = new HashMap<>();
        for(ITownResourceKey townResourceKey : resources.keySet()){
            if(townResourceKey instanceof ItemStackResourceKey itemStackResourceKey) {
                if (get(itemStackResourceKey) > DELTA) {
                    items.put(itemStackResourceKey.getItemStack(), get(itemStackResourceKey));
                }
            }
        }
        return items;
    }

    public Map<ItemStackResourceKey, Double> getAllItemsByResourceAttribute(ItemResourceAttribute itemResourceAttribute){
        Map<ItemStackResourceKey, Double> items = new HashMap<>();
        for(ITownResourceKey townResourceKey : resources.keySet()){
            if(townResourceKey instanceof ItemStackResourceKey itemStackResourceKey) {
                for(ItemStackResourceKey itemKeyOfAttribute : ITEM_RESOURCE_ATTRIBUTE_CACHE.getOrDefault(itemResourceAttribute, new HashSet<>())){
                    if(itemStackResourceKey.equals(itemKeyOfAttribute)){
                        if (get(itemStackResourceKey) > DELTA) {
                            items.put(itemStackResourceKey, get(itemStackResourceKey));
                        }
                        break;
                    }
                }
            }
        }
        return items;
    }

    /**
     * Get all items of the given attribute stored in town.
     * Can't be used to change the resource.
     * @return A map that contains all items of the given attribute stored in town.
     */
    public Map<ItemStack, Double> getAllItems(ItemResourceAttribute attribute){
        Map<ItemStack, Double> items = new HashMap<>();
        for(ItemStackResourceKey itemStackResourceKey : ITEM_RESOURCE_ATTRIBUTE_CACHE.get(attribute)){
            if(get(itemStackResourceKey) > DELTA){
                items.put(itemStackResourceKey.getItemStack(), get(itemStackResourceKey) * getResourceAmount(itemStackResourceKey, attribute));
            }
        }
        return items;
    }

    public Map<VirtualResourceAttribute, Double> getAllVirtualResources() {
        return resources.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof VirtualResourceAttribute)
                .collect(Collectors.toMap(
                        entry -> (VirtualResourceAttribute) entry.getKey(),
                        Map.Entry::getValue
                ));
    }

    public Map<ITownResourceKey , Double> getAllResources(){
        return Map.copyOf(resources);
    }

    /**
     * 获取城镇剩余(未被占用)的容量。
     */
    public double getCapacityLeft(){
        return this.get(VirtualResourceType.MAX_CAPACITY.generateAttribute(0)) - this.occupiedCapacity;
    }

    /**
     * 获取给定的level以上的，某ITownResourceType的资源数量之和。
     */
    public double getAllAboveLevel(ITownResourceType type, int minLevel){
        if(!type.isLevelValid(minLevel)) return 0;
        double sum = 0;
        for(int i=minLevel;i<=type.getMaxLevel();i++){
            sum += get(type.generateAttribute(i));
        }
        return sum;
    }

    /**
     * 获取给定的level之间的，某ITownResourceType的资源数量之和。
     */
    public double getAllBetweenLevel(ITownResourceType type, int minLevel, int maxLevel){
        if(!type.isLevelValid(minLevel) || !type.isLevelValid(maxLevel)) return 0;
        double sum = 0;
        for(int i=minLevel;i<=maxLevel;i++){
            sum += get(type.generateAttribute(i));
        }
        return sum;
    }

    /**
     * Add or subtract resource to the town. Add if amount >= 0, subtract if amount < 0.
     * Only used in this class.
     * Use addUnsafe and costUnsafe in this package.
     * Use methods in TownResourceManager in other classes.
     */
private void addSigned(ITownResourceKey townResourceKey, double amount){
    //System.out.println("resourcesBefore: " + resources);
    if(townResourceKey instanceof ItemStackResourceKey itemStackResourceKey){
        if(itemStackResourceKey.itemStack.isEmpty()) return;
        double amountExist = get(itemStackResourceKey);
        if(amountExist <= DELTA){
            addItemToCache(itemStackResourceKey);
        }
        if(Math.abs(resources.merge(itemStackResourceKey, amount, Double::sum)) <= DELTA){
            resources.remove(itemStackResourceKey);
        }
        this.occupiedCapacity += amount;
    }
    if(townResourceKey instanceof VirtualResourceAttribute virtualResourceKey){
        resources.merge(virtualResourceKey, amount, Double::sum);
        if(virtualResourceKey.type.needCapacity){
            this.occupiedCapacity += amount;
        }
    }
    //System.out.println("addSigned: " + townResourceKey + " " + amount);
    //System.out.println("resources: " + resources);
    }
    private void addSigned(ItemStack pItemStack, double amount){
        addSigned(new ItemStackResourceKey(pItemStack), amount);
    }


    /**
     * Add resource to the town without checking capacity.
     * If you want to change the resource, please use methods in TownResourceManager.
     */
    void addUnsafe(VirtualResourceAttribute key, double amount){
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
    void costUnsafe(VirtualResourceAttribute key, double amount){
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
    void costUnsafe(ItemStackResourceKey itemStack, double amount){
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
    void set(VirtualResourceAttribute key, double amount){
        double different = amount - get(key);
        this.addSigned(key, different);
    }

    /**
     * 遍历存储的所有物品，获取物品对应的ItemResourceAttribute及其数量，加入到缓存中。
     */
    public void updateCache(){
        for(ITownResourceKey key : resources.keySet()){
            if(key instanceof ItemStackResourceKey itemStackResourceKey){
                if(itemStackResourceKey.getItemStack().isEmpty()) continue;
                addItemToCache(itemStackResourceKey);
            }
        }
    }

    /**
     * 读取所欲城镇相关的物品对应资源数量的recipe，并缓存起来。
     */
    public static void loadItemResourceAmounts(){
        for(ItemResourceAmountRecipe recipe : CUtils.filterRecipes(CDistHelper.getRecipeManager(), ItemResourceAmountRecipe.TYPE)){
            for(Map.Entry<ItemStack, Map<TagKey<Item>, Float>> entry : recipe.data.entrySet()){
                ItemStackResourceKey itemStackResourceKey = new ItemStackResourceKey(entry.getKey());
                for(Map.Entry<TagKey<Item>, Float> entry2 : entry.getValue().entrySet()){
                    TagKey< Item> resourceTagKey = entry2.getKey();
                    float amount = entry2.getValue();
                    if(!ITEM_RESOURCE_AMOUNTS.containsKey(itemStackResourceKey)) ITEM_RESOURCE_AMOUNTS.put(itemStackResourceKey, new HashMap<>());
                    ITEM_RESOURCE_AMOUNTS.computeIfAbsent(itemStackResourceKey, k -> new HashMap<>()).put(ItemResourceAttribute.fromTagKey(resourceTagKey), (double)amount);
                }
            }
        }
    }

    /**
     * Remove all items and virtual resources with zero amount from map.
     */
    public void removeZeros(){
        resources.entrySet().removeIf((entry) -> entry.getValue() < DELTA);
    }

    /**
     * 将所有服务资源设置为0
     */
    public void resetAllServices(){
        Arrays.stream(VirtualResourceType.values())
                .filter(type -> type.isService)
                .forEach(type -> set(type.generateAttribute(0),0));
    }

}
