package com.teammoeg.frostedheart.world;

import com.cannolicatfish.rankine.init.RankineBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.template.RuleTest;

public class FHOreFeatureConfig implements IFeatureConfig {
    public static final Codec<FHOreFeatureConfig> CODEC = RecordCodecBuilder.create((p_236568_0_) -> {
        return p_236568_0_.group(RuleTest.CODEC.fieldOf("target").forGetter((config) -> {
            return config.target;
        }), BlockState.CODEC.fieldOf("state").forGetter((config) -> {
            return config.state;
        }), Codec.intRange(0, 64).fieldOf("size").forGetter((config) -> {
            return config.size;
        })).apply(p_236568_0_, FHOreFeatureConfig::new);
    });
    public final RuleTest target;
    public final int size;
    public final BlockState state;

    public FHOreFeatureConfig(RuleTest p_i241989_1_, BlockState state, int size) {
        this.size = size;
        this.state = state;
        this.target = p_i241989_1_;
    }

    public static final class FillerBlockType {
        public static final FHRuleTest magnetite = new FHRuleTest(new Block[]{RankineBlocks.DOLOSTONE.get(), RankineBlocks.CHALK.get(), RankineBlocks.BRECCIA.get(), RankineBlocks.TUFA_LIMESTONE.get(), RankineBlocks.CARBONACEOUS_SHALE.get(), RankineBlocks.COMENDITE.get()});
        public static final FHRuleTest pyrite = new FHRuleTest(new Block[]{RankineBlocks.MICA_SCHIST.get(), RankineBlocks.RHYOLITE.get(), RankineBlocks.KOMATIITE.get(), RankineBlocks.THOLEIITIC_BASALT.get(), RankineBlocks.RED_DACITE.get(), RankineBlocks.BLACK_DACITE.get()});
    }
}
