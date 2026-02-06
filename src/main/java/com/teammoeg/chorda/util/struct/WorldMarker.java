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

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
/**
 * A storage pattern to mark a very custom section of the world,
 * Works like Set&lt;BlockPos&gt;
 * 
 * */
public class WorldMarker {
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
	}
	public static final Codec<WorldMarker> CODEC=RecordCodecBuilder.create(t->t.group(
			CodecUtil.mapCodec("pos", Codec.LONG.xmap(ChunkPos::new, ChunkPos::toLong), "data", ChunkMarker.CODEC).fieldOf("data").forGetter(o->o.poss)
			).apply(t, WorldMarker::new));
	Map<ChunkPos,ChunkMarker> poss=new HashMap<>();
	Function<ChunkPos,ChunkMarker> getter=t->poss.computeIfAbsent(t, o->new ChunkMarker());
	
	public WorldMarker(Map<ChunkPos, ChunkMarker> poss) {
		super();
		this.poss.putAll(poss);
	}
	public void set(BlockPos pos,boolean data) {
		getter.apply(new ChunkPos(pos)).setBit(pos, data);
	}
	public boolean get(BlockPos pos) {
		ChunkPos cp=new ChunkPos(pos);
		ChunkMarker cm=poss.get(cp);
		if(cm==null)
			return false;
		return cm.getBit(pos);
	}
}
