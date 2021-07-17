package com.teammoeg.frostedheart.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.feature.template.BlockMatchRuleTest;
import net.minecraft.world.gen.feature.template.IRuleTestType;
import net.minecraft.world.gen.feature.template.RuleTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FHRuleTest extends RuleTest {
    public final Block[] blocks;
    public final List<RuleTest> list;

    public FHRuleTest(Block[] blocks) {
        this.blocks = blocks;
        this.list = new ArrayList();
        for (Block block : blocks) {
            RuleTest b = new BlockMatchRuleTest(block);
            list.add(b);
        }
    }


    @Override
    public boolean test(BlockState blockState, Random random) {
        for (RuleTest test : list) {
            if (test.test(blockState, random)) {
                return true;
            }
        }
        return list.isEmpty();
    }


    @Override
    protected IRuleTestType<?> getType() {
        return IRuleTestType.ALWAYS_TRUE;
    }
}
