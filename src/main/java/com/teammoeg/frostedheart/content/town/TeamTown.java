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

package com.teammoeg.frostedheart.content.town;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.block.TownBlockEntity;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.AbstractTownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceActionExecutorHandler;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

/**
 * The town for a player team.
 * <p>
 * The TeamTown is only an interface of the underlying TeamTownData.
 * You may use this to access or modify town data.
 */
public class TeamTown implements ITown, ITownWithResidents, ITownWithBuildings {

    /** The town data, actual data stored on disk. */
    TeamTownData data;

    /**
     * Create a new town based on data.
     * @param data can be taken from a player, team, etc.
     *             can also be experimental data.
     * @return the town
     */
    public static TeamTown create(TeamTownData data) {
        return new TeamTown(data);
    }

    /**
     * Get the town for a player.
     * @param player the player
     * @return the town
     */
    public static TeamTown from(Player player) {
        TeamTownData data = CTeamDataManager.get(player).getData(FHSpecialDataTypes.TOWN_DATA);
        return new TeamTown(data);
    }


    /**
     * Default constructor links storage to the town data.
     * @param td the town data
     */
    public TeamTown(TeamTownData td) {
        super();
        this.data = td;
    }



    /**
     * Get the blocks and their worker data.
     */
    public Map<BlockPos, AbstractTownBuilding> getTownBuildings() {
        return data.buildings;
    }

    /**
     * Get the work data of the town block.
     *
     * @param pos position of the block
     * @return the work data
     */
    public Optional<AbstractTownBuilding> getTownBuilding(BlockPos pos) {
        return Optional.ofNullable(data.buildings.get(pos));
    }

    /**
     * Initializes new AbstractTownBuilding from the townBlockEntity entity.
     * Put the data into the map.
     *
     * @param pos position of the block
     * @param townBlockEntity the townBlockEntity entity associated with the block
     */
    public void addTownBlock(BlockPos pos, TownBlockEntity<?> townBlockEntity) {
        ITownBuilding building =townBlockEntity.createBuilding();
        if(building instanceof AbstractTownBuilding abstractTownBuilding){
            data.buildings.put(pos, abstractTownBuilding); // put 时由 ObservableTownMap.onAttach 自动接线
        }
    }

    /**
     * Remove the town block from the map.
     *
     * @param pos position of the block
     */
    public void removeTownBlock(ServerLevel sl,BlockPos pos) {
        AbstractTownBuilding building=data.buildings.remove(pos);
        building.onRemoved(this);
    }

    public Map<UUID, Resident> getResidents() {
        return data.residents;
    }

    public Collection<Resident> getAllResidents(){
        return data.residents.values();
    }

    public Optional<Resident> getResident(UUID id){
        return Optional.ofNullable(data.residents.get(id));
    }

    public boolean addResident(Resident resident) {
        data.residents.put(resident.getUUID(), resident);
        data.allocateHouse();
        if(resident.getHousePos() == null){
            removeResident(resident);
            return false;
        }
        return true;
    }

    public boolean addResident(String firstName, String lastName) {
        return addResident(new Resident(firstName, lastName));
    }

    /**
     * 客户端/服务端通用：判断城镇是否还能再容纳一名居民。
     * 逻辑镜像 {@link TeamTownData#allocateHouse()} 的空闲槽位判定：
     * 存在任一可工作的 {@code HouseBuilding} 仍有空余房屋槽位即可。
     * <p>
     * 容量按居民实际住房归属（{@code Resident.housePos}）计数而非
     * {@code HouseBuilding.getResidentsID()}：后者不在 CODEC 序列化范围，
     * 客户端快照 / 存档重载后恒为空集，不能作为容量依据；此口径与城镇 GUI
     * 显示、{@link TeamTownData#allocateHouse()} 的真实分配结果保持一致。
     * <p>
     * Client/server shared: whether the town can still accommodate one more resident.
     * Counts actual occupancy by {@code Resident.housePos} instead of
     * {@code HouseBuilding.getResidentsID()}, because the latter is not serialized
     * by the CODEC and is always empty on client snapshots / after save reloads;
     * this matches the town GUI display and the real allocation of
     * {@link TeamTownData#allocateHouse()}.
     *
     * @return 可容纳则返回 true / true if another resident can be accommodated
     */
    public boolean canAddResident() {
        // 单次遍历统计各房屋实际入住人数（仅计已分配房屋的居民；null housePos 与原
        // housePos.equals(...) 恒 false 的计数口径一致，不占槽位），再单次遍历建筑
        // 检查空余槽位：O(居民+建筑)，替代原 O(建筑×居民) 的嵌套 stream。
        Map<BlockPos, Integer> occupancy = new HashMap<>();
        for (Resident resident : data.residents.values()) {
            BlockPos housePos = resident.getHousePos();
            if (housePos != null) {
                occupancy.merge(housePos, 1, Integer::sum);
            }
        }
        for (AbstractTownBuilding building : data.buildings.values()) {
            if (building instanceof HouseBuilding house && house.isBuildingWorkable()) {
                if (occupancy.getOrDefault(house.getPos(), 0) < house.getMaxResidents()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean removeResident(UUID id) {
        if(!data.residents.containsKey(id)){
            return false;
        }
        Resident resident = data.residents.get(id);
        if(resident.getHousePos() != null){
            this.getTownBuilding(resident.getHousePos()).ifPresent(
                    building -> {
                        if(building instanceof HouseBuilding houseBuilding){
                            houseBuilding.removeResident(resident);
                        }
                    }
            );
        }
        if(resident.getWorkPos() != null){
            this.getTownBuilding(resident.getWorkPos()).ifPresent(
                    building -> {

                        if(building instanceof AbstractTownResidentWorkBuilding workBuilding){
                            workBuilding.removeResident(resident);
                        }
                    }
            );
        }
        data.residents.remove(id);
        return true;
    }

    /**
     * Remove all resident matching the first and last name.
     * @param firstName the first name
     * @param lastName the last name
     * @return true if the resident was removed
     */
    public boolean removeResident(String firstName, String lastName) {
        // 先收集再删除：避免遍历 entrySet 时直接 remove 导致 ConcurrentModificationException；
        // 并且走 removeResident(UUID) 完整流程，同步清理住房/工作建筑中的引用。
        List<UUID> toRemove = new ArrayList<>();
        for (Entry<UUID, Resident> entry : getResidents().entrySet()) {
            if (entry.getValue().getFirstName().equals(firstName) && entry.getValue().getLastName().equals(lastName)) {
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(this::removeResident);
        return !toRemove.isEmpty();
    }

    /**
     * Get the town name.
     */
    public String getName() {
        return data.name;
    }

    /**
     * Set the town name.
     * @param name the new name
     */
    public void setName(String name) {
        this.data.name = name;
    }

    @Override
    public TeamTownResourceActionExecutorHandler getActionExecutorHandler() {
        return data.resources.actionExecutor;
    }

    public TeamTownResourceHolder getResourceHolder() {
        return data.resources;
    }

    /**
     * Get the daily snapshot history of the town, newest entry last.
     * Used by information GUIs such as the Mayor's Seal.
     *
     * @return unmodifiable view is not guaranteed; treat as read-only
     */
    public List<TownHistoryEntry> getHistory() {
        return data.getHistory();
    }

    //@Override
    public Optional<TeamTownData> getTownData() {
        return Optional.of(data);
    }
	public double maypickTerrainResource(TerrainResourceType type, double d) {
		return data.maypickTerrainResource(type, d);
	}

    //全局地形资源
	public double pickTerrainResource(TerrainResourceType type,double maxPick) {
		return data.pickTerrainResource(type, maxPick);
	}

    //区块型资源
    public double pickTerrainResource(TerrainResourceType type, ChunkPos chunkPos,double maxPick) {
        return data.pickTerrainResource(type,chunkPos,maxPick);
    }

    public double getRemainingTerrainResource(TerrainResourceType type) {
        return data.getRemainingTerrainResource(type);
    }

    public double getRemainingTerrainResource(TerrainResourceType type, ChunkPos chunkPos) {
        return data.getRemainingTerrainResource(type, chunkPos);
    }

    public double getExtractedTerrainResource(TerrainResourceType type, ChunkPos chunkPos) {
        return data.getExtractedTerrainResource(type, chunkPos);
    }

	public void unpickTerrainResource(TerrainResourceType type,double maxPick) {
		data.unpickTerrainResource(type, maxPick);
	}
}
