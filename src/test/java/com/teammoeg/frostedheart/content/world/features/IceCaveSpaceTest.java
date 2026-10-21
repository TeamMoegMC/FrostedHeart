package com.teammoeg.frostedheart.content.world.features;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

class IceCaveSpaceTest {
    private static final BlockPos CENTER = new BlockPos(8, -20, 8);

    @Test
    void actualClearanceDefinesWideHall() {
        Set<BlockPos> air = new HashSet<>();
        for (int y = 0; y < 5; y++) air.add(CENTER.above(y));
        for (Direction direction : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST}) {
            for (int distance = 1; distance <= 4; distance++) {
                air.add(CENTER.relative(direction, distance));
                air.add(CENTER.relative(direction, distance).above());
            }
        }
        assertTrue(IceCaveSpace.isWide(CENTER, air::contains));

        air.remove(CENTER.relative(Direction.EAST, 4));
        assertFalse(IceCaveSpace.isWide(CENTER, air::contains));

        air.add(CENTER.relative(Direction.EAST, 4));
        air.remove(CENTER.above(4));
        assertFalse(IceCaveSpace.isWide(CENTER, air::contains));
    }

    @Test
    void centerIsClampedInsideWritableChunk() {
        BlockPos west = IceCaveSpace.insideChunk(new BlockPos(16, -20, 31), 5);
        assertTrue(west.getX() >= 21 && west.getX() <= 26);
        assertTrue(west.getZ() >= 21 && west.getZ() <= 26);
    }
}
