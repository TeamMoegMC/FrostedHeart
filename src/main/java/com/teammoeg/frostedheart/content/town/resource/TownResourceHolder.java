package com.teammoeg.frostedheart.content.town.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import lombok.Getter;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
    private Map<ItemResourceKey, Map<ItemStack, Double>> itemResources = new HashMap<>();
    private Map<VirtualResourceKey, Double> virtualResources = new HashMap<>();
    @Getter
    private double occupiedCapacity = 0.0;

    public static final Codec<TownResourceHolder> CODEC = RecordCodecBuilder.create(t -> t.group(
            CodecUtil.mapCodec(ItemResourceKey.CODEC, CodecUtil.mapCodec(ItemStack.CODEC, Codec.DOUBLE)).fieldOf("itemResources").forGetter(o->o.itemResources),
            CodecUtil.mapCodec(VirtualResourceKey.CODEC, Codec.DOUBLE).fieldOf("virtualResources").forGetter(o->o.virtualResources),
            Codec.DOUBLE.fieldOf("occupiedCapacity").forGetter(o->o.occupiedCapacity)
            ).apply(t, TownResourceHolder::new)
    );


    public TownResourceHolder() {}
    public TownResourceHolder(Map<ItemResourceKey, Map<ItemStack, Double>> itemResources, Map<VirtualResourceKey, Double> virtualResources, double occupiedCapacity) {
        this.itemResources = itemResources;
        this.virtualResources = virtualResources;
        this.occupiedCapacity = occupiedCapacity;
    }

    public TownResourceHolder(Map<ItemResourceKey, Map<ItemStack, Double>> itemResources, Map<VirtualResourceKey, Double> virtualResources) {
        DoubleAdder adder = new DoubleAdder();
        itemResources.values().forEach(map -> map.values().forEach(adder::add));
        virtualResources.entrySet().stream()
                .filter(entry -> entry.getKey().getType().needCapacity)
                .forEach(entry -> adder.add(entry.getValue()));
        this.itemResources = itemResources;
        this.virtualResources = virtualResources;
        this.occupiedCapacity = adder.doubleValue();
    }

    public double get(ItemStack itemStack){
        itemStack.setCount(1);
        return itemResources.getOrDefault(ItemResourceKey.fromItemStack(itemStack), Collections.emptyMap()).getOrDefault(itemStack, 0.0);
    }

    public double get(ITownResourceKey key){
        if(key instanceof ItemResourceKey){
            DoubleAdder adder = new DoubleAdder();
            itemResources.getOrDefault(key, Collections.emptyMap()).values().forEach(adder::add);
            return adder.doubleValue();
        } else if(key instanceof VirtualResourceKey){
            return virtualResources.getOrDefault(key, 0.0);
        }
        return 0.0;
    }

    /**
     * If you have already gotten the key of item, you can use this method to reduce the times of reading tags from ItemStack
     */
    public double get(ItemResourceKey key, ItemStack itemStack){
        itemStack.setCount(1);
        return itemResources.getOrDefault(key, Collections.emptyMap()).getOrDefault(itemStack, 0.0);
    }

    /**
     * Get all items stored in town.
     * @return A map that contains all items stored in town.
     */
    public Map<ItemStack, Double> getAllItems(){
        Map<ItemStack, Double> resources = new HashMap<>();
        itemResources.forEach((key, map) -> map.forEach((item, amount) -> resources.merge(item, amount, Double::sum)));
        return resources;
    }

    /**
     * Get all items of the given key stored in town.
     * Can't be used to change the resource.
     * @return A map that contains all items of the given key stored in town.
     */
    public Map<ItemStack, Double> getAllItems(ItemResourceKey key){
        return Map.copyOf(itemResources.getOrDefault(key, Collections.emptyMap()));
    }

    public Map<VirtualResourceKey, Double> getAllVirtualResources() {
        return Map.copyOf(virtualResources);
    }

    /**
     * Remove all items and virtual resources with zero amount from map.
     */
    public void removeZeros(){
        for(Map<ItemStack, Double> map : itemResources.values()){
            map.entrySet().removeIf(entry -> entry.getValue() == 0);
        }
        virtualResources.entrySet().removeIf(entry -> entry.getValue() == 0);
    }

    /**
     * Add resource to the town without checking capacity.
     * If you want to change the resource, please use methods in TownResourceManager.
     */
    void addUnsafe(VirtualResourceKey key, double amount){
        virtualResources.merge(key, amount, Double::sum);
        if(key.type.needCapacity){
            this.occupiedCapacity += amount;
        }
    }

    /**
     * Add resource to the town without checking capacity.
     * If you want to change the resource, please use methods in TownResourceManager.
     * If you have already gotten the key of item, you can use this method to reduce the times of reading tags from ItemStack
     */
    void addUnsafe(ItemResourceKey key, ItemStack itemStack, double amount){
        if(itemStack.isEmpty()) return;
        this.occupiedCapacity += amount;
        itemStack.setCount(1);
        itemResources.get(key).merge(itemStack, amount, Double::sum);
    }

    /**
     * Add resource to the town without checking capacity.
     * If you want to change the resource, please use methods in TownResourceManager.
     * @param itemStack ItemStack to add. The count of ItemStack will be ignored.
     * @param amount The amount that will actually be added.
     */
    void addUnsafe(ItemStack itemStack, double amount){
        if(itemStack.isEmpty()) return;
        ItemResourceKey key = ItemResourceKey.fromItemStack(itemStack);
        addUnsafe(key, itemStack, amount);
    }

    /**
     * Add resource to the town without checking capacity.
     * If you want to change the resource, please use methods in TownResourceManager.
     * If you have already gotten the key of item, you can use this method to reduce the times of reading tags from ItemStack
     * @param key The ItemResourceKey of itemStack. Make sure the key is correct, or resource will be added in the wrong place.
     * @param itemStack ItemStack to add. The amount will be added is the count of ItemStack
     */
    void addUnsafe(ItemResourceKey key, ItemStack itemStack){
        if(itemStack.isEmpty()) return;
        double amount = itemStack.getCount();
        addUnsafe(key, itemStack, amount);
    }

    /**
     * Add resource to the town without checking capacity.
     * If you want to change the resource, please use methods in TownResourceManager.
     * @param itemStack ItemStack to add. The amount will be added is the count of ItemStack
     */
    void addUnsafe(ItemStack itemStack){
        if(itemStack.isEmpty()) return;
        double amount = itemStack.getCount();
        addUnsafe(itemStack, amount);
    }

    /**
     * Cost resource from the town without checking resource left.
     * If you want to change the resource, please use methods in TownResourceManager.
     */
    void costUnsafe(VirtualResourceKey key, double amount){
        virtualResources.merge(key, -amount, Double::sum);
        if(key.type.needCapacity){
            this.occupiedCapacity -= amount;
        }
    }

    /**
     * Cost resource from the town without checking resource left.
     * If you want to change the resource, please use methods in TownResourceManager.
     * If you have already gotten the key of item, you can use this method to reduce the times of reading tags from ItemStack
     * @param itemStack ItemStack to cost. The count of ItemStack will be ignored.
     * @param amount The amount that will actually be cost.
     */
    void costUnsafe(ItemResourceKey key, ItemStack itemStack, double amount){
        addUnsafe(key, itemStack, -amount);
    }

    /**
     * Cost resource from the town without checking resource left.
     * If you want to change the resource, please use methods in TownResourceManager.
     * @param itemStack ItemStack to cost. The count of ItemStack will be ignored.
     * @param amount The amount that will actually be cost.
     */
    void costUnsafe(ItemStack itemStack, double amount){
        addUnsafe(itemStack, -amount);
    }

    /**
     * Cost resource from the town without checking resource left.
     * If you want to change the resource, please use methods in TownResourceManager.
     * If you have already gotten the key of item, you can use this method to reduce the times of reading tags from ItemStack
     * @param itemStack ItemStack to cost. The amount will be cost is the count of ItemStack
     */
    void costUnsafe(ItemResourceKey key, ItemStack itemStack){
        if(itemStack.isEmpty()) return;
        double amount = itemStack.getCount();
        costUnsafe(key, itemStack, amount);
    }

    /**
     * Cost resource from the town without checking resource left.
     * If you want to change the resource, please use methods in TownResourceManager.
     * @param itemStack ItemStack to cost. The amount will be cost is the count of ItemStack
     */
    void costUnsafe(ItemStack itemStack){
        if(itemStack.isEmpty()) return;
        double amount = itemStack.getCount();
        costUnsafe(itemStack, amount);
    }

    /**
     * Set resource.
     */
    void set(VirtualResourceKey key, double amount){
        if(key.type.needCapacity){
            double different = amount - get(key);
            this.addUnsafe(key, different);
        }
        else virtualResources.put(key, amount);
    }
}
