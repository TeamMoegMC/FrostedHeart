/*
 * Copyright (c) 2026 TeamMoeg
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
import com.teammoeg.chorda.io.CodecUtil;

import lombok.Getter;

/**
 * 没有对应物品的城镇资源，包括每日重建的 service 和持久虚拟资源。
 * <p>
 * 当前枚举值：
 * <ul>
 *     <li>{@link #MAX_CAPACITY} —— 仓库容量上限（service，不占容量，level 0）。</li>
 *     <li>{@link #TRANSPORT_CAPACITY} —— 运力，代表当天可提供的运输服务能力；由货运站在晨间重建，
 *     不会被单次运输消耗，也不占用仓库容量，level 0。</li>
 * </ul>
 */
public enum VirtualResourceType implements ITownResourceType{
    MAX_CAPACITY(false, true, 0),
    /**
     * 运力：运送物资的能力。
     * 不占仓库容量（{@code needCapacity=false}），是每日重建的 service（{@code isService=true}），无等级
     * （{@code maxLevel=0}）。货运站产出统一走 {@code VirtualResourceAttributeAction} 的 ADD 路径；运输使用方
     * 只登记占用，不通过 COST 消耗该资源。
     */
    TRANSPORT_CAPACITY(false, true, 0);

    /**
     * Service will be reset when working.
     */
    public final boolean isService;
    /**
     * If resource need capacity, when added or costed by TownResourceHolder, the occupied capacity will be changed.
     */
    public final boolean needCapacity;
    /**
     * the largest level this type of resource can be.
     * 0: this type doesn't have level
     * if maxLevel is n, the level range is [0,n]
     */
    @Getter
    public final int maxLevel;

	public static final Codec<VirtualResourceType> CODEC = CodecUtil.enumCodec(VirtualResourceType.class);
    VirtualResourceType(boolean needCapacity, boolean isService, int maxLevel){
        this.needCapacity=needCapacity;
        this.isService=isService;
        this.maxLevel=maxLevel;
    }

    /**
     * 生成这个ItemResourceType的小写字符串。
     * 并非ItemResourceKey.
     * @return 该ItemResourceType名字的小写字符串。
     */
    @Override
    public String getKey() {
        return this.name().toLowerCase();
    }

    /**
     * Generate town resource attribute of this resource type with given level.
     * @param level The level of the resource. Shouldn't be negative or more than max level.
     * @return TownResourceAttribute of this type and given level.
     */
    @Override
    public VirtualResourceAttribute generateAttribute(int level) {
        return VirtualResourceAttribute.of(this, level);
    }

    /**
     * 根据字符串获取VirtualResourceType。大小写均可。
     * 如果字符串不对应任何一个枚举值的名字，可能会引发IllegalArgumentException。
     * @param stringOfType 对应此枚举类中某个字段的字符串
     * @return 对应的ItemResourceType
     */
    public static VirtualResourceType from(String stringOfType) {
        return VirtualResourceType.valueOf(stringOfType.toUpperCase());
    }

}
