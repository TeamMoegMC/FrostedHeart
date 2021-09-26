package com.teammoeg.frostedheart.content.generatort1;

import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.content.generator.BurnerGeneratorTileEntity;

public class T1GeneratorTileEntity extends BurnerGeneratorTileEntity<T1GeneratorTileEntity> {
    public T1GeneratorTileEntity.GeneratorData guiData = new T1GeneratorTileEntity.GeneratorData();

    public T1GeneratorTileEntity(int temperatureLevelIn, int overdriveBoostIn, int rangeLevelIn) {
        super(FHContent.FHMultiblocks.GENERATOR, FHContent.FHTileTypes.GENERATOR_T1.get(), false, temperatureLevelIn, overdriveBoostIn, rangeLevelIn);
    }

}
