package com.teammoeg.frostedheart.content.town.block;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.block.blockscanner.RoomPathfinder.OccupiedCell;

import lombok.Getter;
import net.minecraft.core.BlockPos;

public class OccupiedZoneInfo {
	 public static final Codec<OccupiedZoneInfo> CODEC = RecordCodecBuilder.create(t -> t.group(
			 OccupiedCell.SET_CODEC.optionalFieldOf("occupiedVolume").forGetter(o -> Optional.ofNullable(o.occupiedCells)),
			 BlockPos.CODEC.listOf().optionalFieldOf("error").forGetter(o->Optional.ofNullable(o.errorPos).map(List::copyOf)),
		     Codec.BOOL.optionalFieldOf("overlapped", false).forGetter(o -> o.overlapped),
		     Codec.BOOL.optionalFieldOf("valid", true).forGetter(o -> o.valid)
		     ).apply(t, OccupiedZoneInfo::new)
		 );
	@Getter
	final Set<OccupiedCell> occupiedCells;
	@Nullable
	@Getter
	final Set<BlockPos> errorPos;
	private static final BlockPos MINIMUM=new BlockPos(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE);
	public static final OccupiedZoneInfo EMPTY=new OccupiedZoneInfo(Set.of(),null,false,false) {

		@Override
		public void setOverlapped(boolean overlapped) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setValid(boolean valid) {
			throw new UnsupportedOperationException();
		}
		
	};
	public static OccupiedZoneInfo empty() {
		return EMPTY;
	}
	public OccupiedZoneInfo(OccupiedCell... occupiedCells) {
		this(Set.of(occupiedCells),null);
	}
	public OccupiedZoneInfo(Set<OccupiedCell> occupiedCells) {
		this(occupiedCells,null);
	}
	public OccupiedZoneInfo(BlockPos errorPos,OccupiedCell... occupiedCells) {
		this(Set.of(occupiedCells),Set.of(errorPos));
	}
	public OccupiedZoneInfo(Set<OccupiedCell> occupiedCells,Set<BlockPos> errorPos) {
		super();
		this.occupiedCells = occupiedCells;
		BlockPos curPos=MINIMUM;
		if(occupiedCells.size()>0) {
	        for(OccupiedCell cell:occupiedCells) {
	        	if(curPos.compareTo(cell.getPos())>0) {
	        		curPos=cell.getPos();
	        	}
	        }
		}
		this.errorPos=errorPos;
        signature=curPos;
	}
	public OccupiedZoneInfo(Optional<Set<OccupiedCell>> occupiedCells,Optional<List<BlockPos>> errorPos, boolean overlapped, boolean valid) {
		this(occupiedCells.orElse(Set.of()),errorPos.map(Set::copyOf).orElse(null),overlapped,occupiedCells.isPresent()&&!errorPos.isPresent()?valid:false);
	}
	public OccupiedZoneInfo(Set<OccupiedCell> occupiedCells,Set<BlockPos> errorPos, boolean overlapped, boolean valid) {
		this(occupiedCells,errorPos);
		this.overlapped = overlapped;
		this.valid = valid;
	}
	boolean overlapped=false;
	boolean valid=true;
	@Getter
	BlockPos signature;
	public boolean isOverlapped() {
		return overlapped;
	}
	public void setOverlapped(boolean overlapped) {
		this.overlapped = overlapped;
	}
	public boolean isValid() {
		return valid;
	}
	public void setValid(boolean valid) {
		this.valid = valid;
	}
	@Override
	public int hashCode() {
		return Objects.hash(occupiedCells);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		OccupiedZoneInfo other = (OccupiedZoneInfo) obj;
		return Objects.equals(occupiedCells, other.occupiedCells);
	}
}
