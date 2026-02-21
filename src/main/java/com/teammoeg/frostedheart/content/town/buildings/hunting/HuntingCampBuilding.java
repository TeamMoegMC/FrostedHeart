package com.teammoeg.frostedheart.content.town.buildings.hunting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.block.OccupiedArea;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import net.minecraft.core.BlockPos;

public class HuntingCampBuilding extends AbstractTownBuilding {

    public static final Codec<HuntingCampBuilding> CODEC = RecordCodecBuilder.create(t -> t.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(o -> o.pos),
                    Codec.BOOL.fieldOf("isStructureValid").forGetter(o -> o.isStructureValid),
                    OccupiedArea.CODEC.fieldOf("occupiedArea").forGetter(o -> o.occupiedArea))
            .apply(t, HuntingCampBuilding::new));

    public HuntingCampBuilding(BlockPos pos) {
        super(pos);
    }

    @Override
    public boolean isBuildingWorkable() {
        return super.isBuildingWorkable();
    }


    /**
     * Full constructor matching the CODEC definition for serialization/deserialization.
     * 
     * @param pos the block position
     * @param isStructureValid whether the structure is valid
     * @param occupiedArea the occupied area
     */
    public HuntingCampBuilding(BlockPos pos, boolean isStructureValid, OccupiedArea occupiedArea) {
        super(pos);
        this.isStructureValid = isStructureValid;
        this.occupiedArea = occupiedArea;
    }
}
