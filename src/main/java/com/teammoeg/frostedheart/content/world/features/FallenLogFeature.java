package com.teammoeg.frostedheart.content.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class FallenLogFeature extends Feature<FallenLogConfig> {

    public FallenLogFeature(Codec<FallenLogConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<FallenLogConfig> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        FallenLogConfig config = context.config();
        Direction mainDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        if (level.isEmptyBlock(origin.below())) {
            return false;
        }

        boolean deepStump = random.nextBoolean();
        if (deepStump) {
            tryPlaceLog(level, origin.below(), config, random, Direction.Axis.Y);
        } else if (tryPlaceLog(level, origin, config, random, mainDir.getAxis())){

        }else {
            return false;
        }


        int length = config.length().sample(random);
        boolean placedAny = false;

        for (int i = 1; i < length; i++) {
            BlockPos currentPos = origin.relative(mainDir, i);

            if (i >3 && level.getBlockState(currentPos.below()).canBeReplaced()){
                currentPos = currentPos.below();
            }

            if (tryPlaceLog(level, currentPos, config, random, mainDir.getAxis())) {
                placedAny = true;

                if (i > 2 && i < length - 1) {
                    placeBranches(level, currentPos, mainDir, config, random);
                }
            } else {
                break;
            }
        }

        return placedAny;
    }



    private void placeBranches(WorldGenLevel level, BlockPos trunkPos, Direction mainDir, FallenLogConfig config, RandomSource random) {

        if (random.nextFloat() < 0.3f) {
            Direction branchDir;
            float roll = random.nextFloat();
            if (roll < 0.5f) {
                branchDir = Direction.UP;
            } else {
                branchDir = random.nextBoolean() ? mainDir.getClockWise() : mainDir.getCounterClockWise();
            }
            BlockPos branchPos = trunkPos.relative(branchDir);


            tryPlaceLog(level, branchPos, config, random, branchDir.getAxis());
        }
    }

    private boolean tryPlaceLog(WorldGenLevel level, BlockPos pos, FallenLogConfig config, RandomSource random, Direction.Axis axis) {
        BlockState targetState = level.getBlockState(pos);
        if (targetState.canBeReplaced()||targetState.is(Blocks.SNOW_BLOCK)) {
            BlockState state = config.logProvider().getState(random, pos);

            if (state.hasProperty(RotatedPillarBlock.AXIS)) {
                state = state.setValue(RotatedPillarBlock.AXIS, axis);
            }
            level.setBlock(pos, state, 2);
            return true;
        }
        return false;
    }
}
