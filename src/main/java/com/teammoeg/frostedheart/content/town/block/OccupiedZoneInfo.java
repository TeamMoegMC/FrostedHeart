package com.teammoeg.frostedheart.content.town.block;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.block.blockscanner.RoomPathfinder.OccupiedCell;

import lombok.Getter;
import net.minecraft.core.BlockPos;

public class OccupiedZoneInfo {
	 public static final Codec<OccupiedZoneInfo> CODEC = RecordCodecBuilder.create(t -> t.group(
			 OccupiedCell.SET_CODEC.optionalFieldOf("occupiedVolume").forGetter(o -> Optional.ofNullable(o.occupiedCells)),
		     
		     Codec.BOOL.optionalFieldOf("overlapped", false).forGetter(o -> o.overlapped),
		     Codec.BOOL.optionalFieldOf("valid", true).forGetter(o -> o.valid)
		     ).apply(t, OccupiedZoneInfo::new)
		 );
	 @Getter
	final Set<OccupiedCell> occupiedCells;
	private static final BlockPos MINIMUM=new BlockPos(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE);
	public static final OccupiedZoneInfo EMPTY=new OccupiedZoneInfo(Set.of(),false,false) {

		@Override
		public void setOverlapped(boolean overlapped) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setValid(boolean valid) {
			throw new UnsupportedOperationException();
		}
		
	};
	public OccupiedZoneInfo(Set<OccupiedCell> occupiedCells) {
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
        signature=curPos;
	}
	public OccupiedZoneInfo(Optional<Set<OccupiedCell>> occupiedCells, boolean overlapped, boolean valid) {
		this(occupiedCells.orElse(Set.of()),overlapped,occupiedCells.isPresent()?valid:false);
	}
	public OccupiedZoneInfo(Set<OccupiedCell> occupiedCells, boolean overlapped, boolean valid) {
		this(occupiedCells);
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
