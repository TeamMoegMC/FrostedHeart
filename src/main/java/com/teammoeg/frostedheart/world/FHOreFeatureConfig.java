package com.teammoeg.frostedheart.world;


import com.cannolicatfish.rankine.init.RankineBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
        public static final FHRuleTest magnetite = new FHRuleTest(new Block[]{RankineBlocks.DOLOSTONE.get(), RankineBlocks.CHALK.get(), RankineBlocks.BRECCIA.get(), RankineBlocks.TUFA_LIMESTONE.get(), RankineBlocks.CARBONACEOUS_SHALE.get()});
        public static final FHRuleTest pyrite = new FHRuleTest(new Block[]{RankineBlocks.MICA_SCHIST.get(), RankineBlocks.RHYOLITE.get(), RankineBlocks.KOMATIITE.get(), RankineBlocks.THOLEIITIC_BASALT.get(), RankineBlocks.RED_DACITE.get(), RankineBlocks.BLACK_DACITE.get()});
        public static final FHRuleTest native_copper = new FHRuleTest(new Block[]{Blocks.ANDESITE, RankineBlocks.SLATE.get(), RankineBlocks.THOLEIITIC_BASALT.get(), RankineBlocks.RED_DACITE.get(), RankineBlocks.BLACK_DACITE.get()});
        public static final FHRuleTest malachite = new FHRuleTest(new Block[]{RankineBlocks.TUFA_LIMESTONE.get(), RankineBlocks.WHITE_MARBLE.get(), RankineBlocks.BLACK_MARBLE.get(), RankineBlocks.BRECCIA.get()});
        public static final FHRuleTest pentlandite = new FHRuleTest(new Block[]{Blocks.DIORITE, RankineBlocks.RHYOLITE.get(), RankineBlocks.PYROXENE_GABBRO.get(), RankineBlocks.GRANODIORITE.get(), RankineBlocks.KOMATIITE.get()});
        public static final FHRuleTest native_tin = new FHRuleTest(new Block[]{RankineBlocks.PEGMATITE.get(), RankineBlocks.RHYOLITE.get(), RankineBlocks.QUARTZITE.get(), RankineBlocks.QUARTZ_SANDSTONE.get()});
        public static final FHRuleTest cassiterite = new FHRuleTest(new Block[]{Blocks.DIORITE, RankineBlocks.GRANODIORITE.get(), Blocks.GRANITE, RankineBlocks.GRAY_GRANITE.get(), RankineBlocks.PYROXENE_GABBRO.get(), RankineBlocks.WHITE_MARBLE.get()});
        public static final FHRuleTest bituminous = new FHRuleTest(new Block[]{RankineBlocks.CHALK.get(), RankineBlocks.CARBONACEOUS_SHALE.get(), RankineBlocks.DOLOSTONE.get(), RankineBlocks.TUFA_LIMESTONE.get()});
        public static final FHRuleTest lignite = new FHRuleTest(new Block[]{RankineBlocks.CHALK.get(), RankineBlocks.CARBONACEOUS_SHALE.get(), RankineBlocks.DOLOSTONE.get(), RankineBlocks.TUFA_LIMESTONE.get(), RankineBlocks.BRECCIA.get(), RankineBlocks.MUDSTONE.get()});
        public static final FHRuleTest bauxite = new FHRuleTest(new Block[]{RankineBlocks.TUFA_LIMESTONE.get(), RankineBlocks.MUDSTONE.get(), RankineBlocks.CHALK.get(), RankineBlocks.CARBONACEOUS_SHALE.get(), RankineBlocks.DOLOSTONE.get(), RankineBlocks.TUFA_LIMESTONE.get()});
        public static final FHRuleTest stibnite = new FHRuleTest(new Block[]{RankineBlocks.MICA_SCHIST.get(), RankineBlocks.RHYOLITE.get(), RankineBlocks.KOMATIITE.get(), RankineBlocks.THOLEIITIC_BASALT.get(), RankineBlocks.RED_DACITE.get(), RankineBlocks.BLACK_DACITE.get(), RankineBlocks.GRANODIORITE.get(), RankineBlocks.GRAY_GRANITE.get()});
        public static final FHRuleTest magnesite = new FHRuleTest(new Block[]{RankineBlocks.CHALK.get(), RankineBlocks.CARBONACEOUS_SHALE.get(), RankineBlocks.DOLOSTONE.get(), RankineBlocks.TUFA_LIMESTONE.get(), RankineBlocks.WHITE_MARBLE.get()});
    }
}
