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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.feature.template.BlockMatchRuleTest;
import net.minecraft.world.gen.feature.template.IRuleTestType;
import net.minecraft.world.gen.feature.template.RuleTest;

public class FHRuleTest extends RuleTest {
    public final List<RuleTest> list;

    public FHRuleTest(Block[] blocks) {
        this.list = new ArrayList<>();
        for (Block block : blocks) {
            RuleTest b = new BlockMatchRuleTest(block);
            list.add(b);
        }
    }


    @Override
    protected IRuleTestType<?> getType() {
        return IRuleTestType.ALWAYS_TRUE;
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
}
