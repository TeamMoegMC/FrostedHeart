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
import com.teammoeg.frostedheart.content.town.house.HouseState;
import com.teammoeg.frostedheart.content.town.mine.MineBaseState;
import com.teammoeg.frostedheart.content.town.mine.MineState;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.worker.TownWorkerData;
import com.teammoeg.frostedheart.content.town.worker.WorkOrder;
import com.teammoeg.frostedheart.content.town.worker.WorkerState;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;

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
		
		CodecUtil.mapCodec("pos", BlockPos.CODEC, "data", TownWorkerData.CODEC)
		.optionalFieldOf("blocks", Map.of()).forGetter(o -> o.blocks),
		
		CodecUtil.mapCodec("uuid", UUIDUtil.CODEC, "data", Resident.CODEC)
		.optionalFieldOf("residents", Map.of()).forGetter(o -> o.residents),
		
		CodecUtil.mapCodec("type", CodecUtil.enumCodec(TerrainResourceType.values()),"extracted",Codec.LONG.xmap(ResourceData::new, ResourceData::getExtracted))
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
	Map<BlockPos, TownWorkerData> blocks = new LinkedHashMap<>();

	
	Map<TerrainResourceType,ResourceData> terrainResource=new EnumMap<>(TerrainResourceType.class);
	public TeamTownData(String name, TeamTownResourceHolder resources, Map<BlockPos, TownWorkerData> blocks, Map<UUID, Resident> residents,Map<TerrainResourceType,ResourceData> terrainResource) {
		super();
		this.name = name;
		this.resources = resources;
		this.blocks.putAll(blocks);
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
		this.updateRadius();
		updateAllBlocks(world);
		PriorityQueue<TownWorkerData> pq = new PriorityQueue<>(Comparator.comparingLong(TownWorkerData::getPriority).reversed());
		for (TownWorkerData workerData : blocks.values()) {
			if (AbstractTownWorkerBlockEntity.getStatus(workerData).isValid() && workerData.getType().getWorker() != TownWorker.EMPTY) {
				// 由于已经使用了自动刷新城镇方块的功能，已经不需要通过isWorkValid来在获取合法性信息时刷新。
				// 在抽象类AbstractTownWorkerTileEntity中已经定义了townWorkerState来确定和保存合法性，因此可以直接使用静态方法isValid判断是否是合法的数据
				// 不再使用isWorkValid，从而减少获取TileEntity的次数
				pq.add(workerData);
			}
		}
		// pq.addAll(blocks.values());
		TeamTown teamTown = new TeamTown(this);
		resources.resetAllServices();
		for (WorkOrder order : WorkOrder.values()) {
			for (TownWorkerData t : pq) {
				t.work(teamTown, order);
			}
		}

		// for (TownWorkerData t : pq) {
		// t.setData(world);
		// }
		// 在目前的运行逻辑中，work方法不会改变任何应存储在TileEntity中的信息，因此暂时将此内容放在所有work之前。
		// teamTown.finishWork();此方法已随旧的TownResource一并弃用
	}

	public void tickMorning(ServerLevel world) {
		if (!FHConfig.SERVER.TOWN.enableTownTickMorning.get()) return;
		this.updateAllBlocks(world);
		this.checkOccupiedAreaOverlap();
		this.connectMineAndBase();
		this.tickResidentsMorning();
		this.residentAllocatingCheck();
		this.allocateHouse();
		this.assignWork();
		this.recoverResources();
	}

	void updateAllBlocks(ServerLevel world) {
		Iterator<TownWorkerData> iterator = blocks.values().iterator();
		while (iterator.hasNext()) {
			TownWorkerData data = iterator.next();
			BlockPos pos = data.getPos();
			data.loaded = false;
			if (world.isLoaded(pos)) {
				data.loaded = true;
				BlockState bs = world.getBlockState(pos);
				BlockEntity te = Utils.getExistingTileEntity(world, pos);
				TownWorkerType type = data.getType();
				if (type.getBlock() != bs.getBlock() || !(te instanceof TownBlockEntity)) {
					iterator.remove();
					data.onRemove(world);
				}
			}
		}
	}

	private void checkOccupiedAreaOverlap() {
		// removeNonTownBlocks(world);
		Map<TownWorkerData, OccupiedArea> workersWithOccupiedAreas = blocks.values().stream()
			.map(workerData -> new AbstractMap.SimpleEntry<>(workerData, AbstractTownWorkerBlockEntity.getOccupiedArea(workerData)))
			.filter(entry -> entry.getValue() != null && entry.getValue() != OccupiedArea.EMPTY)
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		ArrayList<TownWorkerData> workerDataList = new ArrayList<>(workersWithOccupiedAreas.keySet());
		Map<TownWorkerData, OccupiedArea> workersMightOverlap = new HashMap<>();

		// 两两比对，根据OccupiedArea的外接矩形是否重合初步筛选可能重叠的worker
		for (int i = 0; i < workerDataList.size() - 1; i++) {
			TownWorkerData workerData = workerDataList.get(i);
			OccupiedArea workerOccupiedArea = workersWithOccupiedAreas.get(workerData);
			for (int j = i + 1; j < workerDataList.size(); j++) {
				TownWorkerData comparingWorkerData = workerDataList.get(j);
				OccupiedArea comparingWorkerOccupiedArea = workersWithOccupiedAreas.get(comparingWorkerData);
				if (workerOccupiedArea.boundingRectangleIntersect(comparingWorkerOccupiedArea)) {
					workersMightOverlap.put(workerData, workerOccupiedArea);
					workersMightOverlap.put(comparingWorkerData, comparingWorkerOccupiedArea);
				}
			}
		}

		Map<ColumnPos, TownWorkerData> occupiedAreaCollectingMap = new HashMap<>();
		Set<TownWorkerData> overlappedWorkers = new HashSet<>();
		// 利用townTileEntity的OccupiedArea判断是否有重叠
		// 遍历所有可能重叠的worker
		for (Map.Entry<TownWorkerData, OccupiedArea> entry : workersMightOverlap.entrySet()) {
			// 遍历该城镇方块所有占用的位置，并与方块本身一起存入occupiedAreaCollectingMap中。
			// 如果发现这个位置已经有过一个worker占用，则将那个worker与这个worker一起存入overlappedWorkers中。
			for (ColumnPos columnPos : entry.getValue().getOccupiedArea()) {
				if (occupiedAreaCollectingMap.containsKey(columnPos)) {
					overlappedWorkers.add(entry.getKey());
					overlappedWorkers.add(occupiedAreaCollectingMap.get(columnPos));
				}
				occupiedAreaCollectingMap.put(columnPos, entry.getKey());
			}
		}
		for (TownWorkerData data : workerDataList) {
			if(overlappedWorkers.contains(data))
				data.getState().status=TownWorkerStatus.OCCUPIED_AREA_OVERLAPPED;
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

	private void residentAllocatingCheck() {
		// 清空residents里所有居民存储的的house和work位置，之后再加回来，以刷新居民的工作和房屋
		residents.values().forEach(resident -> {
			resident.setHousePos(null);
			resident.setWorkPos(null);
		});
		// 移除house/worker里超过上限，或已不存在的的resident
		for (TownWorkerData data : blocks.values()) {
			if (data.getType() == TownWorkerType.HOUSE || data.getType().needsResident()) {
				data.getResidents().removeIf(resident -> !residents.containsKey(resident));
				int maxResident = data.getMaxResident();
				while (data.getResidents().size() > maxResident) {
					data.getResidents().remove(data.getResidents().size()-1);
				}
				for (UUID resident : data.getResidents()) {
					// 把清空的居民的house/work位置设为加回来
					if (data.getType() == TownWorkerType.HOUSE)
						residents.get(resident).setHousePos(data.getPos());
					else residents.get(resident).setWorkPos(data.getPos());
				}

			}
		}
	}

	private void connectMineAndBase() {
		for (TownWorkerData data : blocks.values()) {
			if (data.getType() == TownWorkerType.MINE) {
				((MineState)data.getState()).setConnectedBase(null);
			}
		}
		for (TownWorkerData data : blocks.values()) {
			if (data.getType() == TownWorkerType.MINE_BASE && AbstractTownWorkerBlockEntity.getStatus(data).isValid()) {
				for(BlockPos pos:((MineBaseState)data.getState()).getLinkedMines()) {
					((MineState)blocks.get(pos).getState()).setConnectedBase(data.getPos());
				}
			}
		}
	}

	// distribute homeless residents to house
	void allocateHouse() {
		Iterator<TownWorkerData> houseIterator = blocks.values().stream()
			.filter(data -> data.getType() == TownWorkerType.HOUSE && data.getMaxResident() > data.getResidents().size())
			.sorted(Comparator.comparingDouble(data -> -((HouseState)data.getState()).getRating()))// 优先分配评分最高的house。因此在rating前面加了负号。
			.iterator();
		if (!houseIterator.hasNext()) return;
		TownWorkerData currentHouseData = houseIterator.next();
		Iterator<Resident> residentIterator = residents.values().iterator();
		while (residentIterator.hasNext()) {// 遍历所有居民
			Resident resident = residentIterator.next();
			if (resident.getHousePos() == null) {// 为没有house的居民分配进当前的house(暂存在ListNBT中)
				currentHouseData.addResident(resident.getUUID());
				resident.setHousePos(currentHouseData.getPos());
			}
			if (currentHouseData.getResidents().size() >= currentHouseData.getMaxResident()) {// 如果当前house满了，将暂存在ListNBT中的居民信息存入TownWorkerData，然后尝试进入下一个house
				if (houseIterator.hasNext()) {
					currentHouseData = houseIterator.next();
				} else {
					break;
				}
			}
		}
	}
	private record ResidentScore(Resident resident,double score){
		
	}
	private static final Comparator<ResidentScore> RESIDENT_SCORE_COMPARATOR_DESC=Comparator.<ResidentScore>comparingDouble(t->t.score()).reversed();
	void assignWork() {
		Map<UUID, Resident> availableResidents = residents.values().stream().filter(resident->resident.getWorkPos() == null && resident.getHousePos() != null)
		.collect(Collectors.toMap(t->t.getUUID(), t->t));
		List<TownWorkerData> availableWorkers = blocks.values().stream()
			.filter(data -> data.getType().needsResident())
			.sorted(Comparator.comparingDouble(o -> -o.getResidentPriority()))// 降序排列
			.collect(Collectors.toList());
		if (availableWorkers.isEmpty()) return;
		
		for (TownWorkerData topPriorityWorker:availableWorkers) {
			if (topPriorityWorker.getResidentPriority() == Double.NEGATIVE_INFINITY || availableResidents.isEmpty()) {
				break;// 没有可分的工作或居民，则退出循环，进入保存数据阶段
			}
			TownWorkerType topPriorityWorkerType = topPriorityWorker.getType();
			PriorityQueue<ResidentScore> scoreList=new PriorityQueue<>(availableResidents.size(),RESIDENT_SCORE_COMPARATOR_DESC);
			for (Resident resident:availableResidents.values()) {
				double score = topPriorityWorkerType.getResidentScore(resident);
				if (score == 0.0) {// score为0时，无法进行此类工作，但有可能进行其它工作。
					continue;
				}
				scoreList.add(new ResidentScore(resident,score));
			}
			while (!scoreList.isEmpty()&&topPriorityWorker.getResidents().size()<topPriorityWorker.getMaxResident()) {
				ResidentScore sc=scoreList.poll();
				topPriorityWorker.addResident(sc.resident().getUUID());// 将居民加入worker（暂存，所有循环结束后存入TownWorkerData）
				sc.resident().setWorkPos(topPriorityWorker.getPos());
				availableResidents.remove(sc.resident().getUUID());
			}
		}
	}

	public WorkerState getState(BlockPos worldPosition) {
		return blocks.get(worldPosition).getState();
	}
	private static final Function<TerrainResourceType,ResourceData> rdSupplier=a->new ResourceData();
	public int pickTerrainResource(TerrainResourceType type,int maxPick) {
		ResourceData rd=this.terrainResource.computeIfAbsent(type, rdSupplier);
		long total=Math.min(rd.getRemainResource(), maxPick);
		rd.costResource(total);
		return (int)total;
	}
	public void recoverResources() {
		for(Entry<TerrainResourceType, ResourceData> rd:this.terrainResource.entrySet()) {
			double recover=rd.getValue().getSize()*rd.getKey().getRecoverSpeed();
			rd.getValue().recoverResource(CMath.randomValue(recover));
		}
	}
	public void updateRadius() {
		for(Entry<TerrainResourceType, ResourceData> rd:this.terrainResource.entrySet()) {
			rd.getValue().recalculateRadius(rd.getKey().getResourcePerSq(), 3200);
		}
		
	}
}
