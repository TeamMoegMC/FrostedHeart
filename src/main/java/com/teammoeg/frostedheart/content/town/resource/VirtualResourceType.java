package com.teammoeg.frostedheart.content.town.resource;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import lombok.Getter;

/**
 * 既没有对应物品，又需要长久存储而不在城镇工作时重置的资源。如工具？
 * 此类是否保留仍旧待定。
 */
public enum VirtualResourceType implements ITownResourceType{
    MAX_CAPACITY(false, true, 0);

    //service will be reset when working.
    public final boolean isService;
    public final boolean needCapacity;
    @Getter
    public final int maxLevel;
	public static final Codec<VirtualResourceType> CODEC = CodecUtil.enumCodec(VirtualResourceType.class);
    VirtualResourceType(boolean needCapacity, boolean isService, int maxLevel){
        this.needCapacity=needCapacity;
        this.isService=isService;
        this.maxLevel=maxLevel;
    }

    @Override
    public String getKey() {
        return this.name().toLowerCase();
    }

    @Override
    public VirtualResourceKey generateKey(int level) {
        return VirtualResourceKey.of(this, level);
    }

    public Codec<VirtualResourceType> getCodec() {
        return CODEC;
    }

    public static VirtualResourceType from(String t) {
        return VirtualResourceType.valueOf(t.toUpperCase());
    }

}
