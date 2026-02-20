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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.block.OccupiedArea;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;

import net.minecraft.core.BlockPos;

public class WarehouseBuilding extends AbstractTownBuilding {
	public static final Codec<WarehouseBuilding> CODEC = RecordCodecBuilder.create(t -> t.group(
					BlockPos.CODEC.fieldOf("pos").forGetter(o -> o.pos),
					Codec.BOOL.fieldOf("isStructureValid").forGetter(o -> o.isStructureValid),
					OccupiedArea.CODEC.fieldOf("occupiedArea").forGetter(o -> o.occupiedArea),
					Codec.DOUBLE.fieldOf("capacity").forGetter(o -> o.capacity),
					Codec.INT.fieldOf("area").forGetter(o -> o.area),
					Codec.INT.fieldOf("volume").forGetter(o -> o.volume)
			)
			.apply(t, WarehouseBuilding::new));

    int volume;//有效体积
    int area;//占地面积
    double capacity;//该仓库的最大容量
	public WarehouseBuilding(BlockPos pos) {
        super(pos);
    }

    /**
     * Full constructor matching the CODEC definition for serialization/deserialization.
     * 
     * @param pos the block position
     * @param isStructureValid whether the structure is valid
     * @param occupiedArea the occupied area
     * @param capacity the warehouse capacity
     * @param area the area
     * @param volume the volume
     */
    public WarehouseBuilding(BlockPos pos, boolean isStructureValid, OccupiedArea occupiedArea, double capacity, int area, int volume) {
        super(pos);
        this.isStructureValid = isStructureValid;
        this.occupiedArea = occupiedArea;
        this.capacity = capacity;
        this.area = area;
        this.volume = volume;
    }

	/**
	 * 为城镇添加仓库容量。
	 * 应且只应在城镇清空仓库容量后调用一次。
	 * <br>
	 * 这曾是仓库的work方法，但是我认为考虑到它的特殊性，将它单独分出来了。
	 * @param town 城镇
	 */
	public void addCapacity(Town town) {
		//town.getResourceManager().addIfHaveCapacity(VirtualResourceType.MAX_CAPACITY.generateAttribute(0), capacity);
		TownResourceActions.VirtualResourceAttributeAction action = new TownResourceActions.VirtualResourceAttributeAction(VirtualResourceType.MAX_CAPACITY.generateAttribute(0), capacity, ResourceActionType.ADD, ResourceActionMode.ATTEMPT);
		town.getActionExecutorHandler().execute(action);
	}
}
