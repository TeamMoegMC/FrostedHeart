package com.teammoeg.frostedheart.content.town.buildings.hunting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import net.minecraft.core.BlockPos;

public class HuntingCampBuilding extends AbstractTownBuilding {

    public static final Codec<HuntingCampBuilding> CODEC = RecordCodecBuilder.create(t -> t.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(o -> o.pos),
                    Codec.BOOL.fieldOf("isStructureValid").forGetter(o -> o.isStructureValid),
                    OccupiedVolume.CODEC.fieldOf("occupiedVolume").forGetter(o -> o.occupiedVolume))
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
     * @param occupiedVolume the occupied area
     */
    public HuntingCampBuilding(BlockPos pos, boolean isStructureValid, OccupiedVolume occupiedVolume) {
        super(pos);
        this.isStructureValid = isStructureValid;
        this.occupiedVolume = occupiedVolume;
    }
}
