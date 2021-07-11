package com.teammoeg.frostedheart.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.feature.template.IRuleTestType;
import net.minecraft.world.gen.feature.template.RuleTest;

import java.util.Random;

public class FHRuleTest extends RuleTest {
    public final Block[] blocks;

    public FHRuleTest(Block[] blocks) {
        this.blocks = blocks;

    }


    @Override
    public boolean test(BlockState blockState, Random random) {
        for (Block block : blocks) {
            return blockState.matchesBlock(block);
        }
        return true;
    }


    @Override
    protected IRuleTestType<?> getType() {
        return IRuleTestType.ALWAYS_TRUE;
    }
}
