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
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
                    Codec.INT.optionalFieldOf("decorationAmount",0).forGetter(o -> o.getDecorationAmount()),
                    BlockPos.CODEC.listOf().optionalFieldOf("interfaces", List.of())
                            .forGetter(o -> List.copyOf(o.interfacePositions)),
                    BlockPos.CODEC.listOf().optionalFieldOf("emitters", List.of())
                            .forGetter(o -> List.copyOf(o.emitterPositions))
			)
			.apply(t, WarehouseBuilding::new));

    private int volume;//有效体积
    private int area;//占地面积
    private double capacity;//该仓库的最大容量
    @Getter
    private int decorationAmount;
    private final Set<BlockPos> interfacePositions = new LinkedHashSet<>();
    private final Set<BlockPos> emitterPositions = new LinkedHashSet<>();

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
        this(pos, isStructureValid, occupiedVolume, initialized, occupiedAreaOverlapped, capacity, area, volume, decorationAmount, List.of(), List.of());
    }

    public WarehouseBuilding(BlockPos pos, boolean isStructureValid, OccupiedVolume occupiedVolume, boolean initialized,
                             boolean occupiedAreaOverlapped, double capacity,
                             int area, int volume, int decorationAmount, List<BlockPos> interfacePositions) {
        this(pos, isStructureValid, occupiedVolume, initialized, occupiedAreaOverlapped, capacity, area, volume,
                decorationAmount, interfacePositions, List.of());
    }

    public WarehouseBuilding(BlockPos pos, boolean isStructureValid, OccupiedVolume occupiedVolume, boolean initialized,
                             boolean occupiedAreaOverlapped, double capacity,
                             int area, int volume, int decorationAmount, List<BlockPos> interfacePositions,
                             List<BlockPos> emitterPositions) {
        super(pos);
        this.setIsStructureValid(isStructureValid);
        this.setOccupiedVolume(occupiedVolume);
        this.setInitialized(initialized);
        this.setOccupiedAreaOverlapped(occupiedAreaOverlapped);
        this.setCapacity(capacity);
        this.setArea(area);
        this.setVolume(volume);
        this.setDecorationAmount(decorationAmount);
        replaceInterfaces(interfacePositions);
        replaceEmitters(emitterPositions);
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

    public Set<BlockPos> getInterfacePositions() {
        return Collections.unmodifiableSet(interfacePositions);
    }

    public boolean containsInterface(BlockPos interfacePos) {
        return interfacePositions.contains(interfacePos);
    }

    public Set<BlockPos> replaceInterfaces(Collection<BlockPos> positions) {
        Set<BlockPos> previous = new LinkedHashSet<>(interfacePositions);
        interfacePositions.clear();
        positions.stream().map(BlockPos::immutable).forEach(interfacePositions::add);
        return previous;
    }

    public boolean removeInterface(BlockPos interfacePos) {
        return interfacePositions.remove(interfacePos);
    }

    public Set<BlockPos> getEmitterPositions() {
        return Collections.unmodifiableSet(emitterPositions);
    }

    public boolean containsEmitter(BlockPos emitterPos) {
        return emitterPositions.contains(emitterPos);
    }

    public Set<BlockPos> replaceEmitters(Collection<BlockPos> positions) {
        Set<BlockPos> previous = new LinkedHashSet<>(emitterPositions);
        emitterPositions.clear();
        positions.stream().map(BlockPos::immutable).forEach(emitterPositions::add);
        return previous;
    }

    public boolean removeEmitter(BlockPos emitterPos) {
        return emitterPositions.remove(emitterPos);
    }

    /**
     * Releases watcher-backed devices that are currently loaded before this
     * logical warehouse is detached from the town. Unloaded devices validate
     * the missing warehouse mapping when they next load and stay inoperable.
     */
    public void unbindLoadedDevices(ServerLevel level) {
        for (BlockPos interfacePos : interfacePositions) {
            if (level.isLoaded(interfacePos)
                    && level.getBlockEntity(interfacePos) instanceof WarehouseInterfaceBlockEntity warehouseInterface) {
                warehouseInterface.unbindIfBoundTo(this);
            }
        }
        for (BlockPos emitterPos : emitterPositions) {
            if (level.isLoaded(emitterPos)
                    && level.getBlockEntity(emitterPos) instanceof WarehouseLevelEmitterBlockEntity levelEmitter) {
                levelEmitter.unbindIfBoundTo(this);
            }
        }
        replaceInterfaces(Set.of());
        replaceEmitters(Set.of());
    }
}
