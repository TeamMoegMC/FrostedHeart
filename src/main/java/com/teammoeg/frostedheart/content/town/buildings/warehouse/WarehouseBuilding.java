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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.ITown;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import lombok.Getter;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;

import net.minecraft.core.BlockPos;

public class WarehouseBuilding extends AbstractTownBuilding {
	public static final Codec<WarehouseBuilding> CODEC = RecordCodecBuilder.create(t -> t.group(
                    BlockPos.CODEC.optionalFieldOf("pos",BlockPos.ZERO).forGetter(o -> o.pos),
                    Codec.BOOL.optionalFieldOf("isStructureValid",false).forGetter(o -> o.isStructureValid()),
                    OccupiedVolume.CODEC.optionalFieldOf("occupiedVolume",OccupiedVolume.EMPTY).forGetter(o -> o.getOccupiedVolume()),
                    Codec.BOOL.optionalFieldOf("initialized", false).forGetter(o -> o.isInitialized()),
                    Codec.BOOL.optionalFieldOf("occupiedAreaOverlapped", false).forGetter(o -> o.isOccupiedAreaOverlapped()),
					Codec.DOUBLE.optionalFieldOf("capacity",0D).forGetter(o -> o.getCapacity()),
					Codec.INT.optionalFieldOf("area",0).forGetter(o -> o.getArea()),
					Codec.INT.optionalFieldOf("volume",0).forGetter(o -> o.getVolume()),
                    Codec.INT.optionalFieldOf("decorationAmount",0).forGetter(o -> o.getDecorationAmount())
			)
			.apply(t, WarehouseBuilding::new));

    private int volume;//有效体积
    private int area;//占地面积
    private double capacity;//该仓库的最大容量
    @Getter
    private int decorationAmount;

    public void setVolume(int volume) { if (this.volume == volume) return; this.volume = volume; fireChange(); }
    public void setArea(int area) { if (this.area == area) return; this.area = area; fireChange(); }
    public void setCapacity(double capacity) { if (Double.compare(this.capacity, capacity) == 0) return; this.capacity = capacity; fireChange(); }
    public void setDecorationAmount(int decorationAmount) { if (this.decorationAmount == decorationAmount) return; this.decorationAmount = decorationAmount; fireChange(); }
	public WarehouseBuilding(BlockPos pos) {
        super(pos);
    }

    /**
     * Full constructor matching the CODEC definition for serialization/deserialization.
     * 
     * @param pos the block position
     * @param isStructureValid whether the structure is valid
     * @param occupiedVolume the occupied area
     * @param capacity the warehouse capacity
     * @param area the area
     * @param volume the volume
     */
    public WarehouseBuilding(BlockPos pos, boolean isStructureValid, OccupiedVolume occupiedVolume, boolean initialized,
                             boolean occupiedAreaOverlapped, double capacity, int area, int volume, int decorationAmount) {
        super(pos);
        this.setIsStructureValid(isStructureValid);
        this.setOccupiedVolume(occupiedVolume);
        this.setInitialized(initialized);
        this.setOccupiedAreaOverlapped(occupiedAreaOverlapped);
        this.setCapacity(capacity);
        this.setArea(area);
        this.setVolume(volume);
        this.setDecorationAmount(decorationAmount);
    }

	/**
	 * 为城镇添加仓库容量。
	 * 应且只应在城镇清空仓库容量后调用一次。
	 * <br>
	 * 这曾是仓库的work方法，但是我认为考虑到它的特殊性，将它单独分出来了。
	 * @param town 城镇
	 */
	public void addCapacity(ITown town) {
		//town.getResourceManager().addIfHaveCapacity(VirtualResourceType.MAX_CAPACITY.generateAttribute(0), capacity);
		TownResourceActions.VirtualResourceAttributeAction action = new TownResourceActions.VirtualResourceAttributeAction(VirtualResourceType.MAX_CAPACITY.generateAttribute(0), capacity, ResourceActionType.ADD, ResourceActionMode.ATTEMPT);
		town.getActionExecutorHandler().execute(action);
	}

	public int getVolume() {
		return volume;
	}

	public int getArea() {
		return area;
	}

	public double getCapacity() {
		return capacity;
	}
}
