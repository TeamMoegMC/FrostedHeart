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

package com.teammoeg.chorda.util.struct;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.io.CodecUtil;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
/**
 * 世界标记器，用于标记世界中自定义区域的存储模式。
 * 功能类似于Set&lt;BlockPos&gt;，但使用BitSet按区块和分段存储以节省内存。
 * <p>
 * World marker for marking custom sections of the world.
 * Works like Set&lt;BlockPos&gt; but uses BitSet storage organized by chunk
 * and section for memory efficiency.
 */
public class WorldMarker {
	/**
	 * 区块标记器，管理单个区块内16个分段的方块标记。
	 * <p>
	 * Chunk marker managing block marks across 16 sections within a single chunk.
	 */
	public static class ChunkMarker{
		public static final Codec<ChunkMarker> CODEC=RecordCodecBuilder.create(t->t.group(CodecUtil.discreteList(Codec.LONG_STREAM.xmap(LongStream::toArray, LongStream::of)).fieldOf("data").forGetter(o->o.getList())).apply(t, ChunkMarker::new));
		BitSet[] sections=new BitSet[16];
		
		public ChunkMarker() {
		}
		public ChunkMarker(List<long[]> data) {
			for(int i=0;i<sections.length;i++) {
				long[] ls=data.get(i);
				if(ls==null)continue;
				sections[i]=BitSet.valueOf(ls);
			}
		}
		public BitSet getOrCreateSection(int sec) {
			if(sections[sec]==null)
				sections[sec]=new BitSet(4096);
			return sections[sec];
		}
		public List<long[]> getList(){
			return Stream.of(sections).map(o->o==null?null:o.toLongArray()).collect(Collectors.toList());
		}
		private int getBitIndex(int x,int y,int z) {
			return ((x&15)<<8)+((y&15)<<4)+(z&15);
		}
		public boolean getBit(int x,int y,int z) {
			if(sections[y>>4+4]==null)return false;
			return sections[y>>4+4].get(getBitIndex(x,y,z));
		}
		public void setBit(int x,int y,int z,boolean data) {
			getOrCreateSection(y>>4+4).set(getBitIndex(x,y,z), data);
		}
		public void setBit(BlockPos pos,boolean data) {
			setBit(pos.getX(),pos.getY(),pos.getZ(),data);
		}
		public boolean getBit(BlockPos pos) {
			return getBit(pos.getX(),pos.getY(),pos.getZ());
		}
		public void forEach(ChunkPos chunkPos, java.util.function.Consumer<BlockPos> action) {
			for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
				BitSet section = sections[sectionIndex];
				if (section == null) continue;
				int bitIndex = 0;
				while ((bitIndex = section.nextSetBit(bitIndex)) != -1) {
					int relX = (bitIndex >> 8) & 0xF;
					int relY = ((bitIndex >> 4) & 0xF) + (sectionIndex << 4);
					int relZ = bitIndex & 0xF;
					int absX = chunkPos.getMinBlockX() + relX;
					int absZ = chunkPos.getMinBlockZ() + relZ;
					BlockPos pos = new BlockPos(absX, relY, absZ);
					action.accept(pos);
					bitIndex++;
				}
			}
		}
	}
	public static final Codec<WorldMarker> CODEC=RecordCodecBuilder.create(t->t.group(
			CodecUtil.mapCodec("pos", Codec.LONG.xmap(ChunkPos::new, ChunkPos::toLong), "data", ChunkMarker.CODEC).fieldOf("data").forGetter(o->o.poss)
			).apply(t, WorldMarker::new));
	@Getter
	Map<ChunkPos,ChunkMarker> poss=new HashMap<>();
	Function<ChunkPos,ChunkMarker> getter=t->poss.computeIfAbsent(t, o->new ChunkMarker());
	
	public WorldMarker(Map<ChunkPos, ChunkMarker> poss) {
		super();
		this.poss.putAll(poss);
	}
	/**
	 * 设置指定位置的标记状态。
	 * <p>
	 * Set the mark state at the specified position.
	 *
	 * @param pos 方块位置 / the block position
	 * @param data 标记值 / the mark value
	 * @return 如果标记状态发生变化，则返回true / return true if the mark state changed
	 */
	public boolean set(BlockPos pos,boolean data) {
		ChunkMarker chunkMarker = getter.apply(new ChunkPos(pos));
		boolean old=chunkMarker.getBit(pos);
		if(old!=data) {
			chunkMarker.setBit(pos, data);
			return true;
		}
		return false;
	}
	/**
	 * 获取指定位置的标记状态。
	 * <p>
	 * Get the mark state at the specified position.
	 *
	 * @param pos 方块位置 / the block position
	 * @return 标记值，未标记的位置返回false / the mark value, false for unmarked positions
	 */
	public boolean get(BlockPos pos) {
		ChunkPos cp=new ChunkPos(pos);
		ChunkMarker cm=poss.get(cp);
		if(cm==null)
			return false;
		return cm.getBit(pos);
	}
	/**
	 * 将另一个 WorldMarker 的所有标记合并到当前 WorldMarker。
	 * <p>
	 * Merge all marks from another WorldMarker into this one.
	 *
	 * @param another 要合并的另一个 WorldMarker / another WorldMarker to merge
	 */
	public void merge(WorldMarker another) {
		if (another == null) return;
		for (Map.Entry<ChunkPos, ChunkMarker> entry : another.poss.entrySet()) {
			ChunkPos chunkPos = entry.getKey();
			ChunkMarker otherMarker = entry.getValue();
			ChunkMarker thisMarker = getter.apply(chunkPos);
			for (int i = 0; i < 16; i++) {
				BitSet otherSection = otherMarker.sections[i];
				if (otherSection == null) continue;
				BitSet thisSection = thisMarker.getOrCreateSection(i);
				thisSection.or(otherSection);
			}
		}
	}
	/**
	 * 对当前 WorldMarker 中所有标记的 BlockPos 执行指定操作。
	 * <p>
	 * Perform the specified action on all marked BlockPositions in this WorldMarker.
	 *
	 * @param action 要对每个 BlockPos 执行的操作 / action to perform on each BlockPos
	 */
	public void forEach(java.util.function.Consumer<BlockPos> action) {
		for (Map.Entry<ChunkPos, ChunkMarker> entry : poss.entrySet()) {
			ChunkPos chunkPos = entry.getKey();
			ChunkMarker marker = entry.getValue();
			if (marker == null) continue;
			marker.forEach(chunkPos, action);
		}
	}
	/**
	 * 生成包含当前 WorldMarker 中所有标记 BlockPos 的 Stream。
	 * <p>
	 * Generate a Stream containing all marked BlockPositions in this WorldMarker.
	 *
	 * @return 包含所有标记 BlockPos 的 Stream / Stream containing all marked BlockPositions
	 */
	public Stream<BlockPos> streamPos() {
		Stream.Builder<BlockPos> builder = Stream.builder();
		forEach(builder::add);
		return builder.build();
	}
	/**
	 * 判断当前 WorldMarker 与另一个 WorldMarker 是否存在相同的位置标记。
	 * <p>
	 * Check whether this WorldMarker has any overlapped marked positions with another WorldMarker.
	 *
	 * @param another 另一个 WorldMarker / another WorldMarker to check
	 * @return 如果存在相同的位置标记则返回 true，否则返回 false / true if there are overlapped positions, false otherwise
	 */
	public boolean intersects(WorldMarker another) {
		if (another == null) return false;
		for (Map.Entry<ChunkPos, ChunkMarker> entry : another.poss.entrySet()) {
			ChunkPos chunkPos = entry.getKey();
			ChunkMarker otherMarker = entry.getValue();
			ChunkMarker thisMarker = poss.get(chunkPos);
			if (thisMarker == null) continue;
			for (int i = 0; i < 16; i++) {
				BitSet otherSection = otherMarker.sections[i];
				if (otherSection == null) continue;
				BitSet thisSection = thisMarker.sections[i];
				if (thisSection == null) continue;
				if (thisSection.intersects(otherSection)) {
					return true;
				}
			}
		}
		return false;
	}
}
