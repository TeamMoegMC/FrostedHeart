package com.teammoeg.frostedheart.content.town.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.util.io.CodecUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 提供了对城镇资源进行操作的一些方法。
 */
public class TownResourceManager {
    public final TownResourceHolder resourceHolder;

    public static final Codec<TownResourceManager> CODEC = RecordCodecBuilder.create(t -> t.group(
                    TownResourceHolder.CODEC.fieldOf("holder").forGetter(o->o.resourceHolder)
            ).apply(t, TownResourceManager::new)
    );

    public TownResourceManager(Map<TownResourceKey, Long> resourceMap){
        this.resourceHolder = new TownResourceHolder(resourceMap);
    }

    public TownResourceManager(TownResourceHolder holder){
        this.resourceHolder = holder;
    }

    public TownResourceManager(){
        this.resourceHolder = new TownResourceHolder();
    }


    public long get(TownResourceKey key){
        return resourceHolder.get(key);
    }

    public long getCapacityLeft(){
        return get(TownResourceType.MAX_CAPACITY) - resourceHolder.getOccupiedCapacity();
    }

    private void addUnsafe(TownResourceKey key, long amount){
        resourceHolder.addUnsafe(key,amount);
    }

    private void costUnsafe(TownResourceKey key, long amount){
        resourceHolder.costUnsafe(key,amount);
    }

    private void set(TownResourceKey key,long amount){
        resourceHolder.set(key,amount);
    }

    public long get(TownResourceType type){
        LongAdder sum = new LongAdder();
        TownResourceKey.forEachKeys(type, key -> sum.add(this.get(key)));
        return sum.longValue();
    }

    public long getAllAboveLevel(TownResourceType type,int minLevel){
        LongAdder sum = new LongAdder();
        TownResourceKey.forEachKeysAboveLevel(type, minLevel, key -> sum.add(this.get(key)));
        return sum.longValue();
    }

    /**
     * Add resource to the town if there is enough capacity.
     * @param key the resource key
     * @param amount the amount to add
     * @return if the resource is added
     */
    public boolean addIfHaveCapacity(TownResourceKey key, long amount){
        if(key.type.noSize()){
            addUnsafe(key,amount);
            return true;
        }
        long spaceLeft = getCapacityLeft();
        if(spaceLeft>=amount){
            addUnsafe(key,amount);
            return true;
        }
        return false;
    }

    /**
     * Add resource to the town.
     * If space is not enough, some(same amount with capacity left) of the resource will be added, and others will lose.
     * @param key the resource key
     * @param amount the amount to add
     * @return the amount actually added
     */
    public long addToMax(TownResourceKey key,long amount){
        if(key.type.noSize()){
            addUnsafe(key,amount);
            return amount;
        }
        long capacityLeft = getCapacityLeft();
        if(capacityLeft>=amount){
            addUnsafe(key,amount);
            return amount;
        } else {
            addUnsafe(key,capacityLeft);
            return capacityLeft;
        }
    }

    /**
     * Cost resource from the town.
     * If there is not enough resource, nothing will be cost.
     * @return if the resource is cost
     */
    public boolean costIfHasEnough(TownResourceKey key, long amount){
        if(get(key) >= amount){
            costUnsafe(key,amount);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Cost resource from the town.
     * If there is not enough resource, all resource left will be cost.
     * @return the amount actually cost
     */
    public long costToEmpty(TownResourceKey key,long amount){
        long resource = get(key);
        if(resource>=amount){
            costUnsafe(key,amount);
            return amount;
        } else {
            costUnsafe(key,resource);
            return resource;
        }
    }

    /**
     * Cost resource from the town.
     * 所有此TownResourceType、且等级大于minLevel的资源都可以被消耗。
     * If there is not enough resource, nothing will be cost.
     * @return if the resource is cost
     */
    public boolean costAboveLevelIfHaveEnough(TownResourceType type, long amount, int minLevel){
        long resourceLeft = getAllAboveLevel(type,minLevel);
        if(resourceLeft >= amount) return false;
        AtomicLong resourcesToCost = new AtomicLong(amount);
        TownResourceKey.forEachKeysAboveLevel(type, minLevel, key -> {
            if(resourcesToCost.get()<=0) return;
            long resourcesToCost_1 = resourcesToCost.get();//获取剩余需要扣除的数量
            long costed = this.costToEmpty(key, resourcesToCost_1);//扣除当前等级的资源
            resourcesToCost.set(resourcesToCost_1-costed);//计算并记录剩余需要扣除的数量
        });
        return true;
    }

    public long costAboveLevelToEmpty(TownResourceType type, long amount, int minLevel){
        LongAdder sum = new LongAdder();
        AtomicLong resourcesToCost = new AtomicLong(amount);
        TownResourceKey.forEachKeysAboveLevel(type, minLevel, key -> {
            if(resourcesToCost.get()<=0) return;
            long resourcesToCost_1 = resourcesToCost.get();//获取剩余需要扣除的数量
            long costed = this.costToEmpty(key, resourcesToCost_1);//扣除当前等级的资源
            sum.add(costed);//计算并记录扣除的数量
            resourcesToCost.set(resourcesToCost_1-costed);//计算并记录剩余需要扣除的数量
        });
        return sum.longValue();
    }

    /**
     * Set the amount of resource.
     * 对于需要容量的资源，不建议使用这个方法，因为这可能使资源储量超过上限。
     */
    @Deprecated
    public void set(TownResourceType type, long amount){
        set(TownResourceKey.of(type, false), amount);
    }

    public void resetAllServices(){
        Arrays.stream(TownResourceType.values())
                .filter(type -> type.isService)
                .forEach(type -> set(type,0));
    }

}
