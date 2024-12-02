package com.teammoeg.frostedheart.content.town.resource;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.FHMain;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * Used in the storage of town resource.
 * Holds the resource type and the level.
 * The amount of a resource with specific type and level can be read using this key.
 */
public class TownResourceKey {
    public final TownResourceType type;
    private int level;
    /**
     * Whether the resource is gathered from player or not.
     * If a resource is gathered from player, it shouldn't be gotten by players,
     * because resource of same type can be transformed into different items.
     * It was "backupResource" in old version.
     */
    private boolean isPlayerResource;

    public static final Codec<TownResourceKey> CODEC = RecordCodecBuilder.create(t -> t.group(
            TownResourceType.CODEC.fieldOf("type").forGetter(o->o.type),
            Codec.INT.fieldOf("level").forGetter(o->o.level),
            Codec.BOOL.fieldOf("isPlayerResource").forGetter(o->o.isPlayerResource)
            ).apply(t, TownResourceKey::new)
    );


    TownResourceKey(TownResourceType type, int level, boolean isPlayerResource){
        this.type=type;
        this.isPlayerResource = isPlayerResource;
        if(type.isLevelValid(level)){
            this.level=level;
        } else {
            throw new IllegalArgumentException("Level "+level+" is not valid for resource "+type.getKey());
        }
    }

    TownResourceKey(TownResourceType type, boolean isPlayerResource){
        this.type=type;
        this.isPlayerResource = isPlayerResource;
        this.level = 0;
    }

    //快速生成Key
    public static TownResourceKey of(TownResourceType type, int level, boolean isPlayerResource) {
        return new TownResourceKey(type, level, isPlayerResource);
    }

    public static TownResourceKey of(TownResourceType type, boolean isPlayerResource) {
        return new TownResourceKey(type, isPlayerResource);
    }

    public static TownResourceKey of(TownResourceType type) {
        return new TownResourceKey(type);
    }


    TownResourceKey(TownResourceType type){
        this.type=type;
        if(!type.isService){
            FHMain.LOGGER.info("Resource {} is not service resource, its isPlayerResource is set to false.", type.getKey());
        }
        this.level = 0;
        this.isPlayerResource = false;
    }

    public boolean isService() {
        return type.isService;
    }

    public TownResourceType getType(){
        return type;
    }

    public boolean isPlayerResource() {
        return isPlayerResource;
    }

    public int getLevel() {
        return level;
    }

    /*
      下面的三个方法用于遍历和生成一部分具有特定特征的Key。生成顺序为消耗资源的优先级：等级低者优先，相同等级时玩家资源优先。
     */
    /**
     * 判断是否还有下一个Key
     */
    public boolean hasNext(){
        if(this.isPlayerResource) return true;
        return level<type.maxLevel;
    }

    /**
     * 生成一个新的实例作为下一个Key
     */
    public TownResourceKey getNext(){
        return new TownResourceKey(type,level+(isPlayerResource?0:1),!isPlayerResource);
    }
    /**
     * 将当前Key变为下一个Key
     */
    public void turnToNext(){
        level+=isPlayerResource?0:1;
        this.isPlayerResource = !isPlayerResource;
    }

    public static Collection<TownResourceKey> getAllKeys(TownResourceType type){
        return getAllKeysAboveLevel(type, 0);
    }

    public static Collection<TownResourceKey> getAllKeysAboveLevel(TownResourceType type, int minLevel){
        ArrayList<TownResourceKey> keys = new ArrayList<>();
        TownResourceKey key = new TownResourceKey(type,minLevel,true);
        keys.add(key);
        while(key.hasNext()){
            key = key.getNext();
            keys.add(key);
        }
        return keys;
    }

    public static Collection<TownResourceKey> getAllKeysAtLevel(TownResourceType type, int level){
        ArrayList<TownResourceKey> keys = new ArrayList<>();
        keys.add(new TownResourceKey(type,level,true));
        keys.add(new TownResourceKey(type,level,false));
        return keys;
    }

    public static Collection<TownResourceKey> getAllKeysBetweenLevel(TownResourceType type, int minLevel, int maxLevel){
        ArrayList<TownResourceKey> keys = new ArrayList<>();
        TownResourceKey key = new TownResourceKey(type,minLevel,true);
        keys.add(key);
        while(key.hasNext()){
            key = key.getNext();
            if(key.level>maxLevel) break;
            keys.add(key);
        }
        return keys;
    }

    /**
     * 遍历特定类型,等级在0和type的等级上限之间的的资源，并执行action
     * 来源于玩家或非玩家的资源都会被遍历到
     * 优先遍历等级低的资源，同等级优先遍历玩家资源
     */
    public static void forEachKeys(TownResourceType type, Consumer<TownResourceKey> action){
        forEachKeysAboveLevel(type, 0, action);
    }

    /**
     * 遍历特定类型,等级在minLevel和type的等级上限之间的的资源，并执行action
     * 来源于玩家或非玩家的资源都会被遍历到
     * 优先遍历等级低的资源，同等级优先遍历玩家资源
     */
    public static void forEachKeysAboveLevel(TownResourceType type, int minLevel, Consumer<TownResourceKey> action){
        forEachKeysBetweenLevel(type, minLevel, type.maxLevel, action);
    }

    /**
     * 遍历特定类型,特定等级的资源，并执行action
     * 来源于玩家或非玩家的资源都会被遍历到
     */
    public static void forEachKeysAtLevel(TownResourceType type, int level, Consumer<TownResourceKey> action){
        forEachKeysBetweenLevel(type, level, level, action);
    }

    /**
     * 遍历特定类型,等级在minLevel和maxLevel之间的的资源，并执行action
     * 来源于玩家或非玩家的资源都会被遍历到
     * 优先遍历等级低的资源，同等级优先遍历玩家资源
     */
    public static void forEachKeysBetweenLevel(TownResourceType type, int minLevel, int maxLevel, Consumer<TownResourceKey> action){
        TownResourceKey key = new TownResourceKey(type,minLevel,true);
        while(true){
            action.accept(key);
            if(!key.hasNext()) break;
            if(key.level>maxLevel) break;
            key = key.getNext();
        }
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(o instanceof TownResourceKey otherKey){
            return type==otherKey.type&&level==otherKey.level;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return type.hashCode()*31+level;
    }

}
