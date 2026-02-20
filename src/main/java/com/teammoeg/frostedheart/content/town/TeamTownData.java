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

package com.teammoeg.frostedheart.content.town;

import blusunrize.immersiveengineering.common.util.Utils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataHolder;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.math.CMath;
import com.teammoeg.frostedheart.content.town.block.OccupiedArea;
import com.teammoeg.frostedheart.content.town.block.TownBlockEntity;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceData;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Town data for a whole team.
 * <p>
 * It maintains town resources, worker data, and holds a team data when
 * initialized.
 * <p>
 * Everything permanent should be saved in this class.
 */
public class TeamTownData implements SpecialData {
	public static final Codec<TeamTownData> CODEC = RecordCodecBuilder.create(t -> t.group(
		Codec.STRING.fieldOf("name").forGetter(o -> o.name),
		
		TeamTownResourceHolder.CODEC.fieldOf("resources").forGetter(o -> o.resources),
		
		CodecUtil.mapCodec("pos", BlockPos.CODEC, "building", AbstractTownBuilding.CODEC)
		.optionalFieldOf("blocks", Map.of()).forGetter(
				o -> new HashMap<>(o.buildings)),
		
		CodecUtil.mapCodec("uuid", UUIDUtil.CODEC, "data", Resident.CODEC)
		.optionalFieldOf("residents", Map.of()).forGetter(o -> o.residents),
		
		CodecUtil.mapCodec("type", CodecUtil.enumCodec(TerrainResourceType.values()),"extracted",Codec.DOUBLE.xmap(TerrainResourceData::new, TerrainResourceData::getExtracted))
		.optionalFieldOf("terrainResource", Map.of()).forGetter(o->o.terrainResource)
		)
		
		.apply(t, TeamTownData::new));
//    public static final Codec<TeamTownData> CODEC = CodecUtil.debugCodec(CODEC_TOWN);
	/**
	 * The town name.
	 */
	String name = "Default Town";
	/**
	 * The town residents.
	 */
	Map<UUID, Resident> residents = new LinkedHashMap<>();
	/**
	 * Town resources. Including normal resources and town services. Including
	 * resources gathered from town and resources gathered from player. Must be
	 * changed by TownResourceManager.
	 */
	TeamTownResourceHolder resources = new TeamTownResourceHolder();
	/**
	 * Town blocks and their worker data
	 */
	Map<BlockPos, AbstractTownBuilding> buildings = new LinkedHashMap<>();

	
	Map<TerrainResourceType, TerrainResourceData> terrainResource=new EnumMap<>(TerrainResourceType.class);

	public TeamTownData(String name, TeamTownResourceHolder resources, Map<BlockPos, ITownBuilding> buildings, Map<UUID, Resident> residents, Map<TerrainResourceType, TerrainResourceData> terrainResource) {
		super();
		this.name = name;
		this.resources = resources;
		buildings.forEach((pos, building) -> {
			if(building instanceof AbstractTownBuilding){
				buildings.put(pos, building);
			}
		});
		this.residents.putAll(residents);
		this.terrainResource.putAll(terrainResource);
	}

	public TeamTownData(SpecialDataHolder teamData) {
		super();
		if (teamData instanceof TeamDataHolder data) {

			this.name = data.getTeam().getName() + "'s Town";

		}
	}

	/**
	 * 获取以本实例为data的TeamTown。
	 * 
	 * @return 以本实例为data的TeamTown。
	 */
	public TeamTown createTeamTown() {
		return TeamTown.create(this);
	}

	/**
	 * Town logic update (every 20 ticks). This method first validates the town
	 * blocks, then sorts them by priority and calls the work methods.
	 *
	 * @param world server world instance
	 */
	public void tick(ServerLevel world) {
		if (!FHConfig.SERVER.TOWN.enableTownTick.get()) return;
	}

	public void tickMorning(ServerLevel world) {
		if (!FHConfig.SERVER.TOWN.enableTownTickMorning.get()) return;
		TeamTown town = this.createTeamTown();
		this.checkBlocks(world, town);
		this.checkOccupiedAreaOverlap();
		this.tickResidentsMorning();
		this.residentAllocatingCheck(town);
		this.allocateHouse();
		this.assignWork();
		this.buildingsWork();
		this.recoverResources();
	}

	/**
	 * 检查所有town blocks是否和当前储存的一致
	 */
	void checkBlocks(ServerLevel level, TeamTown town) {
		Iterator<AbstractTownBuilding> iterator = buildings.values().iterator();
		while (iterator.hasNext()) {
			AbstractTownBuilding building = iterator.next();
			BlockPos pos = building.getPos();
			if (level.isLoaded(pos)) {
				//BlockState bs = level.getBlockState(pos);
				BlockEntity blockEntity = Utils.getExistingTileEntity(level, pos);
				if(blockEntity instanceof TownBlockEntity<?> townBlockEntity){
					//这个getBuilding的作用是：当building符合类型时，转变类型，否则返回null。
					// 因此通过它可以判断building是否为BlockEntity对应的Building
					if(townBlockEntity.getBuilding(building) != null){
						continue;
					}
                }
				iterator.remove();
				building.onRemoved(town);
			}
		}
	}

	private void checkOccupiedAreaOverlap() {
		// removeNonTownBlocks(world);
		List<AbstractTownBuilding> buildingsWithOccupiedAreas = buildings.values().stream()
				.filter(building -> building.getOccupiedArea() != null && building.getOccupiedArea() != OccupiedArea.EMPTY)
				.toList();

		Set<AbstractTownBuilding> workersMightOverlap = new HashSet<>();
		// 两两比对，根据OccupiedArea的外接矩形是否重合初步筛选可能重叠的worker
		for (int i = 0; i < buildingsWithOccupiedAreas.size() - 1; i++) {
			AbstractTownBuilding building = buildingsWithOccupiedAreas.get(i);
			OccupiedArea workerOccupiedArea = building.getOccupiedArea();
			for (int j = i + 1; j < buildingsWithOccupiedAreas.size(); j++) {
				AbstractTownBuilding comparingBuilding = buildingsWithOccupiedAreas.get(j);
				OccupiedArea comparingWorkerOccupiedArea = comparingBuilding.getOccupiedArea();
				if (workerOccupiedArea.boundingRectangleIntersect(comparingWorkerOccupiedArea)) {
					workersMightOverlap.add(building);
					workersMightOverlap.add(comparingBuilding);
				}
			}
		}

		Map<ColumnPos, AbstractTownBuilding> occupiedAreaCollectingMap = new HashMap<>();
		Set<AbstractTownBuilding> overlappedWorkers = new HashSet<>();
		// 利用townTileEntity的OccupiedArea判断是否有重叠
		// 遍历所有可能重叠的worker
		for (AbstractTownBuilding building : workersMightOverlap) {
			// 遍历该城镇方块所有占用的位置，并与方块本身一起存入occupiedAreaCollectingMap中。
			// 如果发现这个位置已经有过一个worker占用，则将那个worker与这个worker一起存入overlappedWorkers中。
			for (ColumnPos columnPos : building.getOccupiedArea().getOccupiedArea()) {
				if (occupiedAreaCollectingMap.containsKey(columnPos)) {
					overlappedWorkers.add(building);
					overlappedWorkers.add(occupiedAreaCollectingMap.get(columnPos));
				}
				occupiedAreaCollectingMap.put(columnPos, building);
			}
		}
		for (AbstractTownBuilding building : buildingsWithOccupiedAreas) {
            building.occupiedAreaOverlapped = overlappedWorkers.contains(building);
		}
	}

	/**
	 * 处理村民死亡
	 */
	private void tickResidentsMorning() {
		if (Town.DEBUG_MODE) {
			return;// 测试时村民不死
		}
		List<Resident> deadResidents = new ArrayList<>();
		for (Resident resident : residents.values()) {
			if (resident.getHousePos() == null) {
				resident.costHealth(10);
			}
			if (resident.getHealth() <= 5 || // 似了
				resident.getMental() <= 5) {// 跑了
				deadResidents.add(resident);
			}
		}
		TeamTown town = TeamTown.create(this);
		deadResidents.forEach(resident -> resident.setDeath(town));
	}

	private void residentAllocatingCheck(TeamTown town) {
		// 清空residents里所有居民存储的的house和work位置，之后再加回来，以刷新居民的工作和房屋
		residents.values().forEach(resident -> {
			resident.setHousePos(null);
			resident.setWorkPos(null);
		});
		// 移除house/worker里超过上限，或已不存在的的resident
		for (AbstractTownBuilding building : buildings.values()) {
			if (building instanceof ITownResidentBuilding residentBuilding) {
				residentBuilding.getResidentsID().removeIf(resident -> !residents.containsKey(resident));
				int maxResident = residentBuilding.getMaxResidents();
				while (residentBuilding.getResidentsID().size() > maxResident) {
					residentBuilding.getResidentsID().stream().findAny().ifPresent(uuid -> residents.remove(uuid));
				}
				for (UUID resident : residentBuilding.getResidentsID()) {
					// 把清空的居民的house/work位置设为加回来
					if (building instanceof HouseBuilding){
						residents.get(resident).setHousePos(building.getPos());
					}
					else residents.get(resident).setWorkPos(building.getPos());
				}
			}
		}
	}

	// distribute homeless residents to house
	void allocateHouse() {
		Iterator<HouseBuilding> houseIterator = buildings.values().stream()
				.filter(building -> building instanceof HouseBuilding)
				.map(building -> (HouseBuilding) building)
				.filter(building ->building.getMaxResidents() > building.getResidentsID().size())
			.sorted(Comparator.comparingDouble(building -> -building.getRating()))// 优先分配评分最高的house。因此在rating前面加了负号。
			.iterator();
		if (!houseIterator.hasNext()) return;
		HouseBuilding currentHouseData = houseIterator.next();
        for (Resident resident : residents.values()) {// 遍历所有居民
            if (resident.getHousePos() == null) {// 为没有house的居民分配进当前的house(暂存在ListNBT中)
                currentHouseData.addResident(resident);
            }
            if (currentHouseData.getResidentsID().size() >= currentHouseData.getMaxResidents()) {// 如果当前house满了，将暂存在ListNBT中的居民信息存入TownWorkerData，然后尝试进入下一个house
                if (houseIterator.hasNext()) {
                    currentHouseData = houseIterator.next();
                } else {
                    break;
                }
            }
        }
	}
	private record ResidentScoreCache(@NotNull Resident resident, @NotNull double score){
		
	}
	//private static final Comparator<ResidentScoreCache> RESIDENT_SCORE_COMPARATOR_DESC=Comparator.<ResidentScoreCache>comparingDouble(t->t.score()).reversed();
	void assignWork() {
		Map<UUID, Resident> availableResidents = residents.values().stream().filter(resident->resident.getWorkPos() == null && resident.getHousePos() != null)
		.collect(Collectors.toMap(Resident::getUUID, t->t));
		PriorityQueue<ITownResidentWorkBuilding> availableBuildings = buildings.values().stream()
				.filter(AbstractTownBuilding::isBuildingWorkable)
				.filter(building -> building instanceof ITownResidentWorkBuilding)
				.map(building -> (ITownResidentWorkBuilding) building)
				.sorted(Comparator.comparingDouble(o -> -o.getResidentPriority()))// 降序排列
				.collect(Collectors.toCollection(() -> new PriorityQueue<>(Comparator.comparingDouble(ITownResidentWorkBuilding::getResidentPriority).reversed())));

		Map<ITownResidentWorkBuilding, Map<Resident, Double/*score*/>> buildingResidentScoreCache = new HashMap<>();

		while(!availableBuildings.isEmpty()){
			ITownResidentWorkBuilding topPriorityBuilding = availableBuildings.poll();
			if(topPriorityBuilding.getResidentPriority() == Double.NEGATIVE_INFINITY) break;
			Resident bestResident = null;
			double bestResidentScore = 0;
			Map<Resident, Double> residentScoreCache = buildingResidentScoreCache.computeIfAbsent(topPriorityBuilding, a->new HashMap<>());
			for(Resident resident:availableResidents.values()){
				double residentScore = residentScoreCache.computeIfAbsent(resident, topPriorityBuilding::getResidentScore);
				if(residentScore > bestResidentScore){
					bestResident = resident;
					bestResidentScore = residentScore;
				}
			}
			if(bestResident != null){
				topPriorityBuilding.addResident(bestResident);
				availableResidents.remove(bestResident.getUUID());
			}
			if(topPriorityBuilding.getResidentPriority() != Double.NEGATIVE_INFINITY){
				availableBuildings.add(topPriorityBuilding);
			}
		}
	}

	/**
	 * execute work method of buildings.
	 */
	private void buildingsWork(){
		this.updateRadius();
		//updateAllBlocks(world);

		TeamTown teamTown = new TeamTown(this);
		resources.resetAllServices();
		buildings.values().stream().filter(building -> building instanceof WarehouseBuilding)
				.filter(AbstractTownBuilding::isBuildingWorkable)
				.forEach(building -> ((WarehouseBuilding) building).addCapacity(teamTown));
		buildings.values().stream()
				.filter(AbstractTownBuilding::isBuildingWorkable)
				.sorted(Comparator.comparingInt(AbstractTownBuilding::getWorkPriority).reversed())
				.forEach(building -> building.work(teamTown));
	}

	private static final Function<TerrainResourceType, TerrainResourceData> RESOURCE_DATA_SUPPLIER = a->new TerrainResourceData();
	public double pickTerrainResource(TerrainResourceType type,double maxPick) {
		TerrainResourceData rd=this.terrainResource.computeIfAbsent(type, RESOURCE_DATA_SUPPLIER);
		double total=Math.min(rd.getRemainResource(), maxPick);
		rd.costResource(total);
		return total;
	}
	public void recoverResources() {
		for(Entry<TerrainResourceType, TerrainResourceData> rd:this.terrainResource.entrySet()) {
			double recover=rd.getValue().getSize()*rd.getKey().getRecoverSpeed();
			rd.getValue().recoverResource(CMath.randomValue(recover));
		}
	}
	public void updateRadius() {
		for(Entry<TerrainResourceType, TerrainResourceData> rd:this.terrainResource.entrySet()) {
			rd.getValue().recalculateRadius(rd.getKey().getResourcePerSq(), 3200);
		}
		
	}

	public void unpickTerrainResource(TerrainResourceType type, double maxPick) {
		TerrainResourceData rd=this.terrainResource.computeIfAbsent(type, RESOURCE_DATA_SUPPLIER);
		rd.recoverResource(maxPick);
	}

	public double maypickTerrainResource(TerrainResourceType type, double d) {
		TerrainResourceData rd=this.terrainResource.computeIfAbsent(type, RESOURCE_DATA_SUPPLIER);
		return rd.mayCostResource(d);
	}
}
