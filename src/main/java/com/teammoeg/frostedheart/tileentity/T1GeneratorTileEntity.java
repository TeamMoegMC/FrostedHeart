package com.teammoeg.frostedheart.tileentity;

import com.teammoeg.frostedheart.FHContent;

public class T1GeneratorTileEntity extends BurnerGeneratorTileEntity<T1GeneratorTileEntity> {
    public T1GeneratorTileEntity.GeneratorData guiData = new T1GeneratorTileEntity.GeneratorData();

    public T1GeneratorTileEntity(int temperatureLevelIn, int overdriveBoostIn, int rangeLevelIn) {
        super(FHContent.FHMultiblocks.GENERATOR, FHContent.FHTileTypes.GENERATOR_T1.get(), false, temperatureLevelIn, overdriveBoostIn, rangeLevelIn);
    }

}
