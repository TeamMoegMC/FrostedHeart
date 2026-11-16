package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.knowledge.game.AirflowGame;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;

@GameTestHolder("frostedresearch_m4")
@PrefixGameTestTemplate(false)
public final class AirflowExcavationGameTests {
    @GameTest(batch="airflow_excavation",template="empty",timeoutTicks=20)
    public static void wallsBlockShortcutsAndEveryWinningRouteCrossesCharcoal(GameTestHelper h) {
        var game = new AirflowGame();
        h.assertTrue(!game.won(), "new board requires excavation");
        h.assertTrue(!game.dig(-1, 0) && !game.dig(0, AirflowGame.SIZE), "out-of-map strokes have no effect");
        game.dig(AirflowGame.INLET, 20 * 28 + 11);
        game.dig(20 * 28 + 11, 2 * 28 + 11);
        game.dig(2 * 28 + 11, 2 * 28 + 18);
        h.assertTrue(!game.won(), "digging across the furnace's side wall cannot bypass its bed");
        for (int y = 0; y <= 13; y++) for (int x : new int[]{13,14,22,23})
            h.assertTrue(game.cell(y * 28 + x) == AirflowGame.WALL, "continuous furnace walls cannot be excavated");
        game.dig(20 * 28 + 11, 20 * 28 + 18);
        game.dig(20 * 28 + 18, 14 * 28 + 18);
        h.assertTrue(game.won(), "a continuous three-cell-wide channel reaches the bed and chimney");
        int[] path = game.view().getIntArray("path");
        h.assertTrue(path[0] == AirflowGame.INLET && path[path.length - 1] == AirflowGame.OUTLET
            && Arrays.stream(path).anyMatch(c -> AirflowGame.terrain(c) == AirflowGame.BED), "visual route really crosses the mandatory bed");
        for (int i = 1; i < path.length; i++) h.assertTrue(Math.abs(path[i] % 28 - path[i-1] % 28)
            + Math.abs(path[i] / 28 - path[i-1] / 28) == 1, "route is continuous, without diagonal corner jumps");
        h.assertTrue(!game.dig(27 * 28, 27 * 28 + 27), "won boards do not change");
        h.succeed();
    }

    @GameTest(batch="airflow_excavation",template="empty",timeoutTicks=20)
    public static void partialAndCompletedExcavationSurviveSaveAndOldRotationsResetOnlyBoard(GameTestHelper h) {
        var game = new AirflowGame(); game.dig(AirflowGame.INLET, 20 * 28 + 10);
        var saved = game.save(); var restored = new AirflowGame(); restored.load(saved);
        h.assertTrue(restored.view().equals(game.view()) && !restored.won(), "partial strokes round-trip exactly");
        h.assertTrue(!restored.dig(AirflowGame.INLET, 20 * 28 + 10), "repeated empty-ground strokes do not mutate state");
        restored.dig(20 * 28 + 10, 20 * 28 + 18); restored.dig(20 * 28 + 18, 14 * 28 + 18);
        game.load(restored.save()); h.assertTrue(game.view().equals(restored.view()) && game.won(), "won route and completion survive reload");
        var legacy = new CompoundTag(); legacy.putIntArray("rotations", new int[9]); game.load(legacy);
        h.assertTrue(game.view().getIntArray("dug").length == 0 && !game.won(), "old rotations rebuild a fresh excavation board");
        game.restoreCompletion(); restored.load(game.save());
        h.assertTrue(restored.won(), "desk can retain an already credited research task without requiring replay");
        restored.reset(); h.assertTrue(!restored.won() && restored.view().getIntArray("dug").length == 0, "reset restores the original terrain");
        h.succeed();
    }
}
