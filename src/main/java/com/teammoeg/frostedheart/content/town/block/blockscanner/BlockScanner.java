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

package com.teammoeg.frostedheart.content.town.block.blockscanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.BlockTemperatureModel;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.block.OccupiedZoneInfo;
import com.teammoeg.frostedheart.content.town.block.blockscanner.RoomPathfinder.OccupiedCell;
import com.teammoeg.frostedheart.content.town.block.blockscanner.RoomPathfinder.ReachabilityResult;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;

public class BlockScanner {
    public static class RoomData {
        public final OccupiedZoneInfo occupiedCells;  // 占据格子（从地板上方到天花板下方）
        
        public final Set<BlockPos> neighborCells;  // 邻居格子（占据格子向外一格，不包含占据格子）
        public final Map<BlockPos,BlockState> insideBlockIndex;
        public final Reference2IntMap<BlockState> insideBlocks;
        public final Reference2IntMap<BlockState> neighborBlocks;
        public final List<BlockPos> doors;
        public final int area;
        public final int volume;
		public RoomData(OccupiedZoneInfo occupiedCells, Set<BlockPos> neighborCells,Map<BlockPos,BlockState> insideBlockIndex, Reference2IntMap<BlockState> insideBlocks, Reference2IntMap<BlockState> neighborBlocks,List<BlockPos> doors) {
			super();
			this.occupiedCells = occupiedCells;
			this.neighborCells = neighborCells;
			this.insideBlockIndex=insideBlockIndex;
			this.insideBlocks = insideBlocks;
			this.neighborBlocks = neighborBlocks;
			this.doors=doors;
			area=occupiedCells.getOccupiedCells().size();
			volume=occupiedCells.getOccupiedCells().stream().mapToInt(OccupiedCell::height).sum();
		}
		public RoomData(OccupiedZoneInfo occupiedCells) {
			super();
			this.occupiedCells = occupiedCells;
			this.neighborCells = Set.of();
			this.insideBlockIndex=Map.of();
			this.insideBlocks = Reference2IntMaps.emptyMap();
			this.neighborBlocks = Reference2IntMaps.emptyMap();
			this.doors=List.of();
			area=occupiedCells.getOccupiedCells().size();
			volume=occupiedCells.getOccupiedCells().stream().mapToInt(OccupiedCell::height).sum();
		}
        public RoomData withValidity(boolean valid) {
            return new RoomData(occupiedCells.withValid(valid), neighborCells, insideBlockIndex,
                    insideBlocks, neighborBlocks, doors);
        }

		public boolean isValid() {
			return occupiedCells.isValid();
		}
		@Override
		public int hashCode() {
			return Objects.hash(neighborCells, occupiedCells);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			RoomData other = (RoomData) obj;
			return Objects.equals(neighborCells, other.neighborCells) && Objects.equals(occupiedCells, other.occupiedCells);
		}
		@Override
		public String toString() {
			return "RoomData [occupiedCells=" + occupiedCells + ", neighborCells=" + neighborCells + ", insideBlockIndex=" + insideBlockIndex + ", insideBlocks=" + insideBlocks + ", neighborBlocks="
				+ neighborBlocks + ", doors=" + doors + ", area=" + area + ", volume=" + volume + "]";
		}
		public int countInsideBlock(Predicate<BlockState> predicate) {
			int result=0;
			for(Entry<BlockState> entry:insideBlocks.reference2IntEntrySet()) {
				if(predicate.test(entry.getKey()))
					result += entry.getIntValue();
			}
			return result;
		}
		public int countNeighborBlock(Predicate<BlockState> predicate) {
			int result=0;
			for(Entry<BlockState> entry:neighborBlocks.reference2IntEntrySet()) {
				if(predicate.test(entry.getKey()))
					result += entry.getIntValue();
			}
			return result;
		}
		public Reference2IntMap<BlockState> findInsideBlock(Predicate<BlockState> predicate) {
			Reference2IntMap<BlockState> result=new Reference2IntOpenHashMap<>();
			for(Entry<BlockState> entry:insideBlocks.reference2IntEntrySet()) {
				if(predicate.test(entry.getKey()))
					result.put(entry.getKey(), entry.getIntValue());
			}
			return result;
		}
		public Reference2IntMap<BlockState> findNeighborBlock(Predicate<BlockState> predicate) {
			Reference2IntMap<BlockState> result=new Reference2IntOpenHashMap<>();
			for(Entry<BlockState> entry:neighborBlocks.reference2IntEntrySet()) {
				if(predicate.test(entry.getKey()))
					result.put(entry.getKey(), entry.getIntValue());
			}
			return result;
		}
		public int countAllBlock(Predicate<BlockState> predicate) {
			return countInsideBlock(predicate)+countNeighborBlock(predicate);
		}
		public Reference2IntMap<BlockState> findAllBlock(Predicate<BlockState> predicate) {
			Reference2IntOpenHashMap<BlockState> result=new Reference2IntOpenHashMap<>();
			for(Entry<BlockState> entry:insideBlocks.reference2IntEntrySet()) {
				if(predicate.test(entry.getKey()))
					result.put(entry.getKey(), entry.getIntValue());
			}
			for(Entry<BlockState> entry:neighborBlocks.reference2IntEntrySet()) {
				if(predicate.test(entry.getKey()))
					result.addTo(entry.getKey(), entry.getIntValue());
			}
			return result;
		}
        public float calculateTemperature(Level world) {
            return sampleTemperature(world, occupiedCells);
        }

		public OccupiedVolume calculateOccupiedVolume() {
			OccupiedVolume ou=new OccupiedVolume();
			for(OccupiedCell cell:occupiedCells.getOccupiedCells()) {
				ou.add(cell.pos);
			}
			return ou;
		};
		public Stream<BlockPos> findBlockPosition(Predicate<BlockState> predicate){
			return insideBlockIndex.entrySet().stream().filter(t->predicate.test(t.getValue())).map(t->t.getKey());
		}
    }
    public static float sampleTemperature(Level world, OccupiedZoneInfo geometry) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return Float.NaN;
        }
        LoadedTownWorld loadedWorld = new LoadedTownWorld(serverLevel);
        long samples = 0;
        double total = 0.0;
        for (OccupiedCell cell : geometry.getOccupiedCells()) {
            for (BlockPos position : cell.reachableIterable()) {
                if (!world.hasChunkAt(position) || position.getY() < world.getMinBuildHeight()
                        || position.getY() >= world.getMaxBuildHeight()) {
                    return Float.NaN;
                }
                float natural = sampleNaturalTemperature(serverLevel, loadedWorld, position);
                double temperature = MinecraftThermalInput.gameplayPassiveEnvironment(serverLevel, position, natural);
                if (!loadedWorld.hasCompleteData() || !Double.isFinite(temperature)) {
                    return Float.NaN;
                }
                total += temperature;
                samples++;
            }
        }
        return samples == 0 ? Float.NaN : (float) (total / samples);
    }

    private static float sampleNaturalTemperature(ServerLevel level, LoadedTownWorld loadedWorld, BlockPos pos) {
        // Keep the real dimension and climate while reading biome data without loading terrain.
        float climateAffection = BlockTemperatureModel.climateBlockAffection(
                pos.getY(), FHConfig.SERVER.CLIMATE.climateStoneInterfaceLevel.get(),
                FHConfig.SERVER.CLIMATE.climateSeaLevel.get(),
                FHConfig.SERVER.CLIMATE.blockMaximumClimateAffection.get().floatValue());
        float natural = BlockTemperatureModel.naturalTemperature(
                WorldTemperature.dimension(level), WorldTemperature.biome(loadedWorld, pos),
                WorldTemperature.altitude(level, pos), WorldTemperature.climate(level, pos), climateAffection);
        return Math.max(FHConfig.SERVER.CLIMATE.absoluteZeroCelsius.get().floatValue(), natural);
    }

    @Nullable
    public static RoomData scanRoomDataFromBlock(Level l,BlockPos roomBlockPos) {
        if (!(l instanceof ServerLevel level)) {
            return null;
        }
        LoadedTownWorld world = new LoadedTownWorld(level);
        RoomData result = scanRoomData(world, world, roomBlockPos.above());
        return world.hasCompleteData() ? result : null;
    }
    @Nullable
    public static RoomData scanRoomData(LevelReader r,AbstractWorld world,BlockPos start) {
    	
    	ReachabilityResult positions=RoomPathfinder.findReachable(world, start);
    	//System.out.println(positions);
    	RoomData rd;
    	if(positions.isSucceed()) {
	    	Map<BlockPos,BlockState> insideBlockIndex=new HashMap<>();
	    	Reference2IntOpenHashMap<BlockState> insideBlocks=new Reference2IntOpenHashMap<>();
	    	Reference2IntOpenHashMap<BlockState> neighborBlocks=new Reference2IntOpenHashMap<>();
	    	Set<BlockPos> doors=new HashSet<>();
	        for(BlockPos cpos:positions.neighborCells) {
	        	BlockState block=r.getBlockState(cpos);
	        	if(block.is(BlockTags.DOORS)) {
	        		if(block.hasProperty(DoorBlock.HALF)) {
	        			if(block.getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER)
	        				doors.add(cpos.below());
	        			else
	        				doors.add(cpos);
	        		}else
	    				doors.add(cpos);
	        	}
	        	neighborBlocks.addTo(block, 1);
	        }
	        for(OccupiedCell ocell:positions.occupiedCells) {
	        	for(MutableBlockPos cpos:ocell) {
	        		BlockState block=r.getBlockState(cpos);
	        		if(!block.isAir())
	        			insideBlockIndex.put(cpos.immutable(),block);
	        		insideBlocks.addTo(block, 1);
	        	}
	        }
	        rd =  new RoomData(new OccupiedZoneInfo(positions.occupiedCells),positions.neighborCells,insideBlockIndex,insideBlocks,neighborBlocks,doors.stream().sorted().toList());
	        
    	}else {
    		rd =  new RoomData(new OccupiedZoneInfo(positions.occupiedCells,Set.of(positions.error),false,false));
    	}
        return rd;
    }
}
