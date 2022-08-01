package com.teammoeg.frostedheart;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import com.teammoeg.frostedheart.content.generator.HeatedGeneratorMultiBlock;
import com.teammoeg.frostedheart.content.generator.NormalGeneratorMultiBlock;
import com.teammoeg.frostedheart.content.generator.UnlitHeatedGeneratorMultiBlock;
import com.teammoeg.frostedheart.content.generator.t1.T1GeneratorMultiblock;
import com.teammoeg.frostedheart.content.generator.t2.T2GeneratorMultiblock;
import com.teammoeg.frostedheart.content.steamenergy.radiator.RadiatorMultiblock;
import net.minecraft.block.Block;

public class FHMultiblocks {
    public static IETemplateMultiblock GENERATOR = new T1GeneratorMultiblock();
    public static IETemplateMultiblock GENERATOR_T2 = new T2GeneratorMultiblock();
    public static IETemplateMultiblock RADIATOR = new RadiatorMultiblock();
    public static Block generator = new NormalGeneratorMultiBlock("generator", FHTileTypes.GENERATOR_T1);
    public static Block generator_t2 = new HeatedGeneratorMultiBlock("generator_t2", FHTileTypes.GENERATOR_T2);
    public static Block radiator = new UnlitHeatedGeneratorMultiBlock("heat_radiator", FHTileTypes.RADIATOR);

    public static void init() {
        MultiblockHandler.registerMultiblock(FHMultiblocks.GENERATOR);
        MultiblockHandler.registerMultiblock(FHMultiblocks.RADIATOR);
        MultiblockHandler.registerMultiblock(FHMultiblocks.GENERATOR_T2);
    }
}