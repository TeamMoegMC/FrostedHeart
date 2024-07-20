/*
 * Copyright (c) 2021-2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.world.geology.ore;


import com.cannolicatfish.rankine.init.RankineBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.template.RuleTest;

public class FHOreFeatureConfig implements IFeatureConfig {
    public static final class FillerBlockType {
        public static final FHRuleTest magnetite = new FHRuleTest(new Block[]{RankineBlocks.DOLOSTONE.get(), RankineBlocks.CHALK.get(), RankineBlocks.SILTSTONE.get(), RankineBlocks.LIMESTONE.get(), RankineBlocks.SHALE.get()});
        public static final FHRuleTest pyrite = new FHRuleTest(new Block[]{RankineBlocks.MICA_SCHIST.get(), RankineBlocks.RHYOLITE.get(), RankineBlocks.KOMATIITE.get(), RankineBlocks.THOLEIITIC_BASALT.get(), RankineBlocks.RED_DACITE.get(), RankineBlocks.BLACK_DACITE.get()});
        public static final FHRuleTest hematite = new FHRuleTest(new Block[]{Blocks.ANDESITE, RankineBlocks.SILTSTONE.get(), RankineBlocks.MUDSTONE.get(), RankineBlocks.SLATE.get(), RankineBlocks.GNEISS.get()});
        public static final FHRuleTest chalcocite = new FHRuleTest(new Block[]{Blocks.ANDESITE, RankineBlocks.SLATE.get(), RankineBlocks.THOLEIITIC_BASALT.get(), RankineBlocks.RED_DACITE.get(), RankineBlocks.BLACK_DACITE.get()});
        public static final FHRuleTest malachite = new FHRuleTest(new Block[]{RankineBlocks.LIMESTONE.get(), RankineBlocks.WHITE_MARBLE.get(), RankineBlocks.BLACK_MARBLE.get(), RankineBlocks.SILTSTONE.get()});
        public static final FHRuleTest pentlandite = new FHRuleTest(new Block[]{RankineBlocks.THOLEIITIC_BASALT.get(), RankineBlocks.GRAY_GRANITE.get(), Blocks.DIORITE, Blocks.GRANITE, RankineBlocks.RHYOLITE.get(), RankineBlocks.GABBRO.get(), RankineBlocks.GRANODIORITE.get(), RankineBlocks.KOMATIITE.get()});
        public static final FHRuleTest native_tin = new FHRuleTest(new Block[]{RankineBlocks.PEGMATITE.get(), RankineBlocks.RHYOLITE.get(), RankineBlocks.QUARTZITE.get(), Blocks.ANDESITE, RankineBlocks.BLACK_MARBLE.get()});
        public static final FHRuleTest cassiterite = new FHRuleTest(new Block[]{Blocks.DIORITE, RankineBlocks.GRANODIORITE.get(), Blocks.GRANITE, RankineBlocks.GRAY_GRANITE.get(), RankineBlocks.GABBRO.get(), RankineBlocks.WHITE_MARBLE.get()});
        public static final FHRuleTest bituminous = new FHRuleTest(new Block[]{RankineBlocks.DOLOSTONE.get(), RankineBlocks.CHALK.get(), RankineBlocks.LIMESTONE.get(), RankineBlocks.MUDSTONE.get()});
        public static final FHRuleTest lignite = new FHRuleTest(new Block[]{RankineBlocks.CHALK.get(), RankineBlocks.SHALE.get(), RankineBlocks.DOLOSTONE.get(), RankineBlocks.LIMESTONE.get(), RankineBlocks.SILTSTONE.get(), RankineBlocks.MUDSTONE.get()});
        public static final FHRuleTest bauxite = new FHRuleTest(new Block[]{RankineBlocks.LIMESTONE.get(), RankineBlocks.MUDSTONE.get(), RankineBlocks.CHALK.get(), RankineBlocks.SHALE.get(), RankineBlocks.DOLOSTONE.get()});
        public static final FHRuleTest stibnite = new FHRuleTest(new Block[]{RankineBlocks.MICA_SCHIST.get(), RankineBlocks.RHYOLITE.get(), RankineBlocks.KOMATIITE.get(), RankineBlocks.THOLEIITIC_BASALT.get(), RankineBlocks.RED_DACITE.get(), RankineBlocks.BLACK_DACITE.get(), RankineBlocks.GRANODIORITE.get(), RankineBlocks.GRAY_GRANITE.get()});
        public static final FHRuleTest magnesite = new FHRuleTest(new Block[]{RankineBlocks.CHALK.get(), RankineBlocks.SHALE.get(), RankineBlocks.DOLOSTONE.get(), RankineBlocks.LIMESTONE.get(), RankineBlocks.WHITE_MARBLE.get()});
        public static final FHRuleTest gold = new FHRuleTest(new Block[]{Blocks.DIORITE, RankineBlocks.MICA_SCHIST.get(), RankineBlocks.RHYOLITE.get(), RankineBlocks.KOMATIITE.get(), RankineBlocks.THOLEIITIC_BASALT.get(), Blocks.GRANITE, RankineBlocks.PEGMATITE.get()});
        public static final FHRuleTest anthracite = new FHRuleTest(new Block[]{RankineBlocks.WHITE_MARBLE.get(), RankineBlocks.SLATE.get()});
        public static final FHRuleTest graphite = new FHRuleTest(new Block[]{RankineBlocks.BLACK_MARBLE.get(), RankineBlocks.MICA_SCHIST.get(), RankineBlocks.SLATE.get(), RankineBlocks.SKARN.get(), RankineBlocks.PEGMATITE.get(), RankineBlocks.GNEISS.get()});
    }
    public static final Codec<FHOreFeatureConfig> CODEC = RecordCodecBuilder.create((p_236568_0_) -> p_236568_0_.group(RuleTest.CODEC.fieldOf("target").forGetter((config) -> config.target), BlockState.CODEC.fieldOf("state").forGetter((config) -> config.state), Codec.intRange(0, 64).fieldOf("size").forGetter((config) -> config.size)).apply(p_236568_0_, FHOreFeatureConfig::new));
    public final RuleTest target;
    public final int size;

    public final BlockState state;

    public FHOreFeatureConfig(RuleTest p_i241989_1_, BlockState state, int size) {
        this.size = size;
        this.state = state;
        this.target = p_i241989_1_;
    }
}
