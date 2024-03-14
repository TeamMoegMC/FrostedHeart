package com.teammoeg.frostedheart.util;

import java.util.ArrayList;
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
import com.teammoeg.frostedheart.util.io.SerializeUtil;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class WorldMarker {
	public static class ChunkMarker{
		public static final Codec<ChunkMarker> CODEC=RecordCodecBuilder.create(t->t.group(Codec.list(SerializeUtil.nullableCodecValue(Codec.LONG_STREAM, null)).fieldOf("data").forGetter(o->o.getList())).apply(t, ChunkMarker::new));
		BitSet[] sections=new BitSet[16];
		
		public ChunkMarker() {
		}
		public ChunkMarker(List<LongStream> data) {
			for(int i=0;i<sections.length;i++) {
				LongStream ls=data.get(i);
				if(ls==null)continue;
				sections[i]=BitSet.valueOf(ls.toArray());
			}
		}
		public BitSet getOrCreateSection(int sec) {
			if(sections[sec]==null)
				sections[sec]=new BitSet(4096);
			return sections[sec];
		}
		public List<LongStream> getList(){
			return Stream.of(sections).map(o->o==null?null:LongStream.of(o.toLongArray())).collect(Collectors.toList());
		}
		private int getBitIndex(int x,int y,int z) {
			return ((x&15)<<8)+((y&15)<<4)+(z&15);
		}
		public boolean getBit(int x,int y,int z) {
			if(y<0||y>=256)return false;
			if(sections[y>>4]==null)return false;
			return sections[y>>4].get(getBitIndex(x,y,z));
		}
		public void setBit(int x,int y,int z,boolean data) {
			if(y<0||y>=256)return;
			getOrCreateSection(y>>4).set(getBitIndex(x,y,z), data);
		}
		public void setBit(BlockPos pos,boolean data) {
			setBit(pos.getX(),pos.getY(),pos.getZ(),data);
		}
		public boolean getBit(BlockPos pos) {
			return getBit(pos.getX(),pos.getY(),pos.getZ());
		}
	}
	public static final Codec<WorldMarker> CODEC=RecordCodecBuilder.create(t->t.group(
			SerializeUtil.mapCodec("pos", Codec.LONG.xmap(ChunkPos::new, ChunkPos::asLong), "data", ChunkMarker.CODEC).fieldOf("data").forGetter(o->o.poss)
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
