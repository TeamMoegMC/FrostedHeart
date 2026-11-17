package com.teammoeg.frostedresearch.gui.drawdesk.game;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.authlib.GameProfile;
import com.teammoeg.chorda.dataholders.team.TeamsAPI;
import com.teammoeg.chorda.dataholders.team.SinglePlayerTeamAPIProvider;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.research.clues.MinigameClue;
import com.teammoeg.frostedresearch.knowledge.KnowledgeService;
import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeElement;
import com.teammoeg.frostedresearch.knowledge.state.*;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@GameTestHolder("frostedresearch_matching")
@PrefixGameTestTemplate(false)
public final class MatchingDifficultyGameTests {
    private static final int[] CARD_COUNTS = {9, 13, 23, 29, 39, 53, 65, 81};
    private static final ResourceLocation DEBUG = new ResourceLocation("frostedresearch:matching_debug");

    @GameTest(batch="matching_generation", template="empty", timeoutTicks=40)
    public static void everyDifficultyHasOrderedNumbersAndPlayableSolutions(GameTestHelper h) {
        long start = System.nanoTime();
        for (int numbers = 0; numbers < 8; numbers++) {
            int level = GenerateInfo.levelForNumbers(numbers);
            for (int seed = 0; seed < 256; seed++) {
                var game = new ResearchGame();
                var solution = game.initWithSolution(GenerateInfo.all[level], new Random(seed));
                game.setLvl(level);
                Set<Integer> actual = new TreeSet<>();
                int cards = 0;
                for (int x = 0; x < 9; x++) for (int y = 0; y < 9; y++) {
                    var card = game.get(x, y);
                    if (!card.isShow()) continue;
                    cards++;
                    if (card.getCt() == CardType.ADDING && card.getCard() != 0 && card.getCard() != 8)
                        actual.add(card.getCard());
                }
                Set<Integer> expected = new TreeSet<>();
                for (int number = 1; number <= numbers; number++) expected.add(number);
                h.assertTrue(actual.equals(expected) && cards == CARD_COUNTS[numbers],
                        "exact numbers and strictly increasing card counts: numbers=" + numbers + ", seed=" + seed);
                // Play a separately restored board through ordinary moves, including the final tile.
                var restored = new ResearchGame(game);
                for (var pair : solution) {
                    h.assertTrue(!pair.getFirst().equals(pair.getSecond())
                            && restored.isTouchable(pair.getFirst()) && restored.isTouchable(pair.getSecond())
                            && restored.tryCombine(pair.getFirst(), pair.getSecond()), "each generated move is legal");
                    if (restored.addcur > 1)
                        restored = new ResearchGame(restored);
                }
                h.assertTrue(restored.tryCombine(ResearchGame.FINAL_CARD, null) && restored.isFinished(),
                        "all 2048 restored games reach a normal win, without forceWin");
            }
        }
        System.out.println("MATCHING: 2048 boards solved after NBT restoration in "
                + (System.nanoTime() - start) / 1_000_000 + " ms");
        h.succeed();
    }

    @GameTest(batch="matching_generation", template="empty", timeoutTicks=40)
    public static void impossibleRandomLayoutFallsBackToSolvableBoard(GameTestHelper h) {
        for (var original : GenerateInfo.all) {
            var impossible = new GenerateInfo(original.numElms, original.numPairs, original.numWild, original.numAdd)
                    .generators((cards, random) -> {
                        for (var column : cards) for (var card : column) card.unplacable = true;
                    });
            var game = new ResearchGame();
            var solution = game.initWithSolution(impossible, new Random(17));
            game.setLvl(GenerateInfo.levelForNumbers(original.numAdd));
            h.assertTrue(solution.size() * 2 + 1 == CARD_COUNTS[original.numAdd], "fallback keeps the requested workload");
            for (var pair : solution)
                h.assertTrue(game.tryCombine(pair.getFirst(), pair.getSecond()), "fallback ordinary moves are legal");
            h.assertTrue(game.tryCombine(ResearchGame.FINAL_CARD, null) && game.isFinished(), "fallback really wins");
        }
        h.succeed();
    }

    @GameTest(batch="matching_generation", template="empty", timeoutTicks=40)
    public static void difficultyComparisonsUseNumbersAndKeepLegacyToolTiers(GameTestHelper h) {
        for (int played = 0; played < 8; played++) for (int required = 0; required < 8; required++)
            h.assertTrue(GenerateInfo.atLeast(GenerateInfo.levelForNumbers(played), GenerateInfo.levelForNumbers(required))
                    == (played >= required), "difficulty follows numbers, never historical ID order");
        for (int level = 0; level < 4; level++)
            h.assertTrue(GenerateInfo.numberCount(level) == level + 4 && GenerateInfo.toolLevel(level) == level,
                    "legacy definitions, wins, paper and pens keep their meanings");
        for (int level = 4; level < 8; level++)
            h.assertTrue(GenerateInfo.toolLevel(level) == 0, "starter puzzles use basic paper and charcoal pencil");
        h.assertTrue(!GenerateInfo.atLeast(-1, 4) && !GenerateInfo.atLeast(8, 4), "unknown IDs give no credit");
        var game = new ResearchGame();
        var solution = game.initWithSolution(GenerateInfo.all[4], new Random(1));
        game.setLvl(4);
        var card = solution.get(0).getFirst();
        h.assertTrue(!game.canCombine(card, card) && !game.tryCombine(card, card), "one tile cannot match itself");
        h.succeed();
    }

    private static <T> T resource(String kind, Codec<T> codec) {
        String path = "/data/frostedresearch/frostedresearch/knowledge/" + kind + "/matching_debug.json";
        try (var reader = new InputStreamReader(Objects.requireNonNull(
                MatchingDifficultyGameTests.class.getResourceAsStream(path)), StandardCharsets.UTF_8)) {
            return codec.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow(false, error -> {});
        } catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }

    @GameTest(batch="matching_debug", template="empty", timeoutTicks=40)
    public static void extendedLegacyCluesAndBasicDeskEquipmentRemainUsable(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current();
        var previousTeams = TeamsAPI.getAPI();
        TeamsAPI.register(new SinglePlayerTeamAPIProvider());
        try {
            var project = resource("projects", ProjectDefinition.CODEC);
            KnowledgeDefinitions.install(new KnowledgeDefinitions.Snapshot(0,
                    Map.of(DEBUG, resource("ideas", IdeaDefinition.CODEC)),
                    Map.of(DEBUG, resource("results", ResultDefinition.CODEC)), Map.of(), Map.of(), Map.of(DEBUG, project), List.of()));
            var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                    new GameProfile(UUID.randomUUID(), "matching-test"));
            var service = KnowledgeService.forPlayer(player);
            service.learn(KnowledgeElement.idea(DEBUG), new AcquisitionSource("command", Optional.empty(), 1), KnowledgeService.Grant.COMMAND);
            var run = service.startResearch(DEBUG).orElseThrow();
            var desk = new DrawingDeskTileEntity(BlockPos.ZERO, FRContents.Blocks.DRAWING_DESK.get().defaultBlockState());
            desk.setLevel(h.getLevel());
            for (int numbers = 0; numbers < 8; numbers++) {
                int level = GenerateInfo.levelForNumbers(numbers);
                String json = "{\"id\":\"test\",\"value\":1,\"level\":" + level + "}";
                var clue = MinigameClue.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result().orElseThrow();
                h.assertTrue(clue.getLevel() == level, "legacy clue codec accepts each stable ID");
                desk.setCalculation(run, "numbers_" + numbers, level);
                desk.getInventory().setStackInSlot(DrawingDeskTileEntity.PAPER_SLOT, new ItemStack(Items.STONE_PRESSURE_PLATE, 2));
                desk.getInventory().setStackInSlot(DrawingDeskTileEntity.INK_SLOT, new ItemStack(FRContents.Items.charcoal.get()));
                h.assertTrue(desk.initGame(player) == (numbers <= 4), "actual desk enforces the expected basic tool tier");
                h.assertTrue(desk.getInventory().getStackInSlot(DrawingDeskTileEntity.PAPER_SLOT).getCount() == (numbers <= 4 ? 1 : 2),
                        "paper is consumed only on a successful start");
                if (numbers <= 4) {
                    var moves = desk.getGame().initWithSolution(GenerateInfo.all[level], new Random(numbers));
                    desk.getGame().setLvl(level);
                    for (var pair : moves)
                        h.assertTrue(desk.tryCombine(player, pair.getFirst(), pair.getSecond()), "basic pencil pays for ordinary matching moves");
                    h.assertTrue(desk.tryCombine(player, ResearchGame.FINAL_CARD, null), "final centre click works normally");
                    desk.updateGame(player);
                    h.assertTrue(KnowledgeService.calculationsDone(service.state().research().get(run), project.requirement("numbers_" + numbers).orElseThrow()) == 1,
                            "desk win reaches the correct debug task");
                }
            }
            h.assertTrue(MinigameClue.CODEC.codec().parse(JsonOps.INSTANCE,
                    JsonParser.parseString("{\"id\":\"invalid\",\"value\":1,\"level\":8}")).error().isPresent(), "unsupported clue remains rejected");
        } finally { KnowledgeDefinitions.install(previous); TeamsAPI.register(previousTeams); }
        h.succeed();
    }

    @GameTest(batch="matching_debug", template="empty", timeoutTicks=40)
    public static void debugProjectIsOptInAndCreditsActualDifficultyAfterReload(GameTestHelper h) {
        var previous = KnowledgeDefinitions.current();
        try {
            var project = resource("projects", ProjectDefinition.CODEC);
            var idea = resource("ideas", IdeaDefinition.CODEC);
            var result = resource("results", ResultDefinition.CODEC);
            var definitions = new KnowledgeDefinitions.Snapshot(0, Map.of(DEBUG, idea), Map.of(DEBUG, result),
                    Map.of(), Map.of(), Map.of(DEBUG, project), List.of());
            h.assertTrue(KnowledgeDefinitions.validate(definitions).isEmpty(), "shipped debug data validates");
            KnowledgeDefinitions.install(definitions);
            var state = new KnowledgeState();
            var service = new KnowledgeService(UUID.randomUUID(), state, 1, () -> {});
            h.assertTrue(!service.projectAccess(DEBUG).discovered() && service.startResearch(DEBUG).isEmpty(),
                    "ordinary players do not discover the debug research");
            service.learn(KnowledgeElement.idea(DEBUG), new AcquisitionSource("command", Optional.empty(), 1), KnowledgeService.Grant.COMMAND);
            var run = service.startResearch(DEBUG).orElseThrow();
            for (int numbers = 0; numbers < 8; numbers++) {
                var task = project.requirement("numbers_" + numbers).orElseThrow();
                var errors = new ArrayList<String>(); task.validate(errors);
                h.assertTrue(errors.isEmpty(), "every task accepts its new or legacy level");
                if (numbers > 0)
                    h.assertTrue(service.recordCalculation(run, task.id(), GenerateInfo.levelForNumbers(numbers - 1)).status()
                            == KnowledgeService.Status.INVALID_INPUT, "easier wins cannot satisfy harder tasks");
                for (int count = 0; count < 16; count++)
                    h.assertTrue(service.recordCalculation(run, task.id(), task.level()).succeeded(), "16 real per-task credits");
                var restored = ResearchHistory.CODEC.parse(NbtOps.INSTANCE,
                        ResearchHistory.CODEC.encodeStart(NbtOps.INSTANCE, state.research().get(run)).result().orElseThrow())
                        .result().orElseThrow();
                h.assertTrue(KnowledgeService.calculationsDone(restored, task) == 16
                        && service.calculationLevel(run, task.id()).isEmpty(), "wins keep their difficulty after persistence");
            }
            h.assertTrue(service.finishResearch(run).get(0).succeeded(), "debug finding can finish normally");
        } finally { KnowledgeDefinitions.install(previous); }
        h.succeed();
    }
}
