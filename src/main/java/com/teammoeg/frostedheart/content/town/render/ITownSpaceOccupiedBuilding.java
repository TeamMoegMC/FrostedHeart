package com.teammoeg.frostedheart.content.town.render;

import java.util.Set;

import com.teammoeg.frostedheart.content.town.block.blockscanner.RoomPathfinder.OccupiedCell;

public interface ITownSpaceOccupiedBuilding {
	public Set<OccupiedCell> getOccupiedVolume();
}
