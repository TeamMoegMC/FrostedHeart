package com.teammoeg.frostedheart.content.town.block.blockscanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.block.blockscanner.RoomPathfinder.OccupiedCell;
import com.teammoeg.frostedheart.content.town.block.blockscanner.RoomPathfinder.ReachabilityResult;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class BlockScanner {
    public static class RoomData {
        public final Set<OccupiedCell> occupiedCells;  // 占据格子（从地板上方到天花板下方）
        
        public final Set<BlockPos> neighborCells;  // 邻居格子（占据格子向外一格，不包含占据格子）
        public final Map<BlockPos,BlockState> insideBlockIndex;
        public final Reference2IntMap<BlockState> insideBlocks;
        public final Reference2IntMap<BlockState> neighborBlocks;
        public final List<BlockPos> doors;
        public final int area;
        public final int volume;
		public RoomData(Set<OccupiedCell> occupiedCells, Set<BlockPos> neighborCells,Map<BlockPos,BlockState> insideBlockIndex, Reference2IntMap<BlockState> insideBlocks, Reference2IntMap<BlockState> neighborBlocks,List<BlockPos> doors) {
			super();
			this.occupiedCells = occupiedCells;
			this.neighborCells = neighborCells;
			this.insideBlockIndex=insideBlockIndex;
			this.insideBlocks = insideBlocks;
			this.neighborBlocks = neighborBlocks;
			this.doors=doors;
			area=occupiedCells.size();
			volume=occupiedCells.stream().mapToInt(OccupiedCell::height).sum();
		}
		@Override
		public String toString() {
			return "RoomData [occupiedCells=" + occupiedCells + ", neighborCells=" + neighborCells + ", insideBlocks=" + insideBlocks + ", neighborBlocks=" + neighborBlocks + "]";
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
			int volume=0;
			double temperature=0;
			for(OccupiedCell cell:occupiedCells)
				for(BlockPos pos:cell.reachableIterable()) {
					temperature+=WorldTemperature.block(world, pos);
					volume++;
				}
			return (float) (temperature/volume);
		}
		public OccupiedVolume calculateOccupiedVolume() {
			OccupiedVolume ou=new OccupiedVolume();
			for(OccupiedCell cell:occupiedCells) {
				ou.add(cell.pos);
			}
			return ou;
		};
		public Stream<BlockPos> findBlockPosition(Predicate<BlockState> predicate){
			return insideBlockIndex.entrySet().stream().filter(t->predicate.test(t.getValue())).map(t->t.getKey());
		}
    }
    @Nullable
    public static RoomData scanRoomDataFromBlock(Level l,BlockPos roomBlockPos) {
    	return scanRoomData(l,roomBlockPos.above());
    	
    }
    @Nullable
    public static RoomData scanRoomData(Level l,BlockPos start) {
    	AbstractWorld world=new CachedWorld(new TownLevelWorld(l));
    	ReachabilityResult positions=RoomPathfinder.findReachable(world, start);
    	if(positions==null)
    		return null;
    	Map<BlockPos,BlockState> insideBlockIndex=new HashMap<>();
    	Reference2IntOpenHashMap<BlockState> insideBlocks=new Reference2IntOpenHashMap<>();
    	Reference2IntOpenHashMap<BlockState> neighborBlocks=new Reference2IntOpenHashMap<>();
    	Set<BlockPos> doors=new HashSet<>();
        for(BlockPos cpos:positions.neighborCells) {
        	BlockState block=l.getBlockState(cpos);
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
        		BlockState block=l.getBlockState(cpos);
        		if(!block.isAir())
        			insideBlockIndex.put(cpos.immutable(),block);
        		insideBlocks.addTo(block, 1);
        	}
        }
        return new RoomData(positions.occupiedCells,positions.neighborCells,insideBlockIndex,insideBlocks,neighborBlocks,new ArrayList<>(doors));
    }
}
