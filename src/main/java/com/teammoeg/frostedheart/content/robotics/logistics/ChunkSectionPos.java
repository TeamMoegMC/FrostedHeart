package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.Objects;

import net.minecraft.util.math.BlockPos;

public class ChunkSectionPos {
	int x;
	int y;
	int z;
	public ChunkSectionPos(BlockPos pos) {
		this(pos.getX()>>4,pos.getY()>>4,pos.getZ()>>4);
	}
	public ChunkSectionPos(int x, int y, int z) {
		super();
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public ChunkSectionPos(long data) {
		this(BlockPos.unpackX(data),BlockPos.unpackY(data),BlockPos.unpackZ(data));
	}
	public long asLong() {
		return BlockPos.pack(x, y, z);
	}
	@Override
	public int hashCode() {
		return Objects.hash(x, y, z);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ChunkSectionPos other = (ChunkSectionPos) obj;
		return x == other.x && y == other.y && z == other.z;
	}

}
