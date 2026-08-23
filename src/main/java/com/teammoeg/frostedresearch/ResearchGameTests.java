/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch;

import com.mojang.authlib.GameProfile;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.SinglePlayerTeamAPIProvider;
import com.teammoeg.chorda.dataholders.team.TeamDataClosure;
import com.teammoeg.chorda.dataholders.team.TeamsAPI;
import com.teammoeg.chorda.dataholders.team.TeamsAPIProvider;
import com.teammoeg.frostedresearch.api.ResearchDataAPI;
import com.teammoeg.frostedresearch.blocks.MechCalcTileEntity;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.handler.ResearchCommonEvents;
import com.teammoeg.frostedresearch.mixinutil.IOwnerTile;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.clues.Clue;
import net.minecraft.core.BlockPos;
import net.minecraft.advancements.Advancement;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@GameTestHolder(FRMain.MODID)
@PrefixGameTestTemplate(false)
public final class ResearchGameTests {
    private ResearchGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void mechanicalCalculatorClaimsAndEnforcesTeamOwner(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, FRContents.Blocks.MECHANICAL_CALCULATOR.get());
        helper.assertTrue(helper.getBlockEntity(pos) instanceof MechCalcTileEntity,
                "mechanical calculator must create its real block entity");
        MechCalcTileEntity calculator = (MechCalcTileEntity) helper.getBlockEntity(pos);
        CompoundTag points = new CompoundTag();
        points.putInt("pts", 75);
        calculator.load(points);

        ServerPlayer fake = FakePlayerFactory.getMinecraft(helper.getLevel());
        helper.assertTrue(calculator.fetchPoint(fake, 10) == 0,
                "FakePlayer must not extract points");
        helper.assertTrue(IOwnerTile.getOwner(calculator) == null,
                "FakePlayer must not claim an ownerless calculator");

        TeamsAPIProvider previousProvider = TeamsAPI.getAPI();
        TeamsAPI.register(new SinglePlayerTeamAPIProvider());
        try {
            ServerPlayer owner = testPlayer(helper);
            helper.assertTrue(calculator.fetchPoint(owner, 30) == 30,
                    "first real player interaction must claim and partially extract");
            helper.assertTrue(CTeamDataManager.get(owner).getId().equals(IOwnerTile.getOwner(calculator)),
                    "calculator owner must be the interacting player's team");
            helper.assertTrue(calculator.getFetchablePoints(owner) == 45,
                    "partial extraction must retain the remaining points");
            helper.assertTrue(calculator.fetchPoint(owner, 0) == 0,
                    "non-positive extraction limits must not mutate the cache");

            ServerPlayer otherTeam = testPlayer(helper);
            helper.assertTrue(calculator.fetchPoint(otherTeam, 45) == 0,
                    "a different team must not extract the cache");
            helper.assertTrue(calculator.getFetchablePoints(owner) == 45,
                    "denied extraction must leave the cache unchanged");
            verifyInvalidCatalogReloadPreservesLiveDefinitions(helper);
            verifyListenerAndEffectLifecycle(helper, owner, otherTeam);
            helper.succeed();
        } finally {
            TeamsAPI.register(previousProvider);
        }
    }

    private static void verifyListenerAndEffectLifecycle(
            GameTestHelper helper, ServerPlayer owner, ServerPlayer otherTeam) {
        Path catalog = FMLPaths.CONFIGDIR.get().resolve("fhresearches");
        Path listenersFile = catalog.resolve("listener_lifecycle.json");
        Path effectFile = catalog.resolve("effect_reset.json");
        try {
            Files.writeString(listenersFile, listenerDefinition());
            Files.writeString(effectFile, effectDefinition());
            helper.assertTrue(FHResearch.reloadCatalog(),
                    "valid listener/effect catalogue must reload");

            Research listeners = FHResearch.getResearch("listener_lifecycle");
            Research effectReset = FHResearch.getResearch("effect_reset");
            helper.assertTrue(listeners != null && effectReset != null,
                    "reloaded catalogue must expose listener and effect definitions");
            helper.assertTrue(ResearchHooks.getKillClues().size() == 1,
                    "one always-on kill listener must be registered globally");
            helper.assertTrue(ResearchHooks.getTickClues().isEmpty(),
                    "non-always advancement listener must wait for an active team");

            TeamDataClosure<TeamResearchData> ownerData = ResearchDataAPI.getData(owner);
            TeamDataClosure<TeamResearchData> otherData = ResearchDataAPI.getData(otherTeam);
            ownerData.get().getData(listeners).setActive();
            ownerData.get().setCurrentResearch(ownerData.team(), listeners);
            helper.assertTrue(ResearchHooks.getKillClues().size() == 2,
                    "activating one team must add its scoped kill listener");
            helper.assertTrue(ResearchHooks.getTickClues().size() == 1,
                    "activating one team must add its scoped advancement listener");

            Clue teamKill = clue(listeners, "team-kill");
            Clue alwaysKill = clue(listeners, "always-kill");
            Clue advancementClue = clue(listeners, "advancement");
            postKill(helper, otherTeam, EntityType.ZOMBIE);
            helper.assertTrue(!ownerData.get().isClueCompleted(listeners, teamKill)
                            && !otherData.get().isClueCompleted(listeners, teamKill),
                    "a team-scoped kill listener must ignore another team");

            postKill(helper, otherTeam, EntityType.SKELETON);
            helper.assertTrue(otherData.get().isClueCompleted(listeners, alwaysKill)
                            && !ownerData.get().isClueCompleted(listeners, alwaysKill),
                    "an always listener must remain global but update only the triggering team");

            postKill(helper, owner, EntityType.ZOMBIE);
            helper.assertTrue(ownerData.get().isClueCompleted(listeners, teamKill),
                    "the owning team's real kill event must complete its listener");

            Advancement advancement = helper.getLevel().getServer().getAdvancements()
                    .getAdvancement(new ResourceLocation("minecraft:story/root"));
            helper.assertTrue(advancement != null, "vanilla root advancement must exist");
            for (String criterion : advancement.getCriteria().keySet()) {
                owner.getAdvancements().award(advancement, criterion);
            }
            ResearchCommonEvents.tickResearch(
                    new TickEvent.PlayerTickEvent(TickEvent.Phase.START, owner));
            helper.assertTrue(ownerData.get().isClueCompleted(listeners, advancementClue),
                    "the real advancement tick listener must complete for its owning team");

            ownerData.get().getData(effectReset).setActive();
            helper.assertTrue(ownerData.get().doResearch(ownerData.team(), effectReset, 100) == 0,
                    "effect research must consume its required points");
            helper.assertTrue(ownerData.get().getData(effectReset).isCompleted(),
                    "effect research must complete through the real team data path");
            helper.assertTrue(ownerData.get().getVariants().getDouble("gametest_stat") == 5.0D,
                    "completion must grant the reversible stat effect");
            helper.assertTrue(ownerData.get().getCurrentResearchValue() == listeners,
                    "completing a non-current research must preserve the current selection");
            ownerData.get().resetData(ownerData.team(), effectReset);
            helper.assertTrue(ownerData.get().getVariants().getDouble("gametest_stat") == 0.0D
                            && !ownerData.get().getData(effectReset).isCompleted(),
                    "research reset must revoke the reversible effect");

            helper.assertTrue(FHResearch.reloadCatalog(),
                    "reloading the same listener catalogue must succeed");
            ownerData.get().initResearch(ownerData.team());
            otherData.get().initResearch(otherData.team());
            helper.assertTrue(ResearchHooks.getKillClues().size() == 1
                            && ResearchHooks.getTickClues().isEmpty(),
                    "reload must rebuild listeners without duplicates or completed team listeners");

            Files.deleteIfExists(listenersFile);
            Files.deleteIfExists(effectFile);
            helper.assertTrue(FHResearch.reloadCatalog(),
                    "removing test definitions must reload the root catalogue");
            ownerData.get().initResearch(ownerData.team());
            otherData.get().initResearch(otherData.team());
            helper.assertTrue(ResearchHooks.getKillClues().isEmpty()
                            && ResearchHooks.getTickClues().isEmpty(),
                    "removed definitions must leave no listener behind");
            helper.assertTrue(ownerData.get().getData("listener_lifecycle")
                            .isClueTriggered("team-kill"),
                    "removed definition progress must remain preserved as orphan data");
        } catch (IOException e) {
            throw new IllegalStateException("Could not exercise listener/effect catalogue", e);
        } finally {
            try {
                Files.deleteIfExists(listenersFile);
                Files.deleteIfExists(effectFile);
                FHResearch.reloadCatalog();
            } catch (IOException e) {
                throw new IllegalStateException("Could not restore the GameTest catalogue", e);
            }
        }
    }

    private static Clue clue(Research research, String nonce) {
        return research.getClues().stream()
                .filter(candidate -> nonce.equals(candidate.getNonce()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing GameTest clue " + nonce));
    }

    private static void postKill(
            GameTestHelper helper, ServerPlayer player, EntityType<? extends LivingEntity> entityType) {
        LivingEntity killed = entityType.create(helper.getLevel());
        helper.assertTrue(killed != null, "test kill target must be constructible");
        ResearchCommonEvents.onPlayerKill(
                new LivingDeathEvent(killed, player.damageSources().playerAttack(player)));
    }

    private static String listenerDefinition() {
        return """
                {
                  "icon":{"type":"item","item":{"id":"minecraft:paper","Count":1}},
                  "category":"frostedresearch:rescue",
                  "parents":[],
                  "clues":[
                    {"type":"kill","id":"team-kill","required":true,"value":0.0,"always":false,"entity":"minecraft:zombie"},
                    {"type":"kill","id":"always-kill","required":false,"value":0.0,"always":true,"entity":"minecraft:skeleton"},
                    {"type":"advancement","id":"advancement","required":true,"value":0.0,"always":false,"advancement":"minecraft:story/root"}
                  ],
                  "ingredients":[],
                  "effects":[],
                  "points":100,
                  "insight":0
                }
                """;
    }

    private static String effectDefinition() {
        return """
                {
                  "icon":{"type":"item","item":{"id":"minecraft:paper","Count":1}},
                  "category":"frostedresearch:rescue",
                  "parents":[],
                  "clues":[],
                  "ingredients":[],
                  "effects":[
                    {"type":"stats","id":"stats","vars":"gametest_stat","val":5.0,"percent":false}
                  ],
                  "points":100,
                  "insight":0
                }
                """;
    }

    private static void verifyInvalidCatalogReloadPreservesLiveDefinitions(GameTestHelper helper) {
        Research liveRoot = FHResearch.getResearch("root");
        helper.assertTrue(liveRoot != null, "isolated production catalogue must contain root");
        Path invalid = FMLPaths.CONFIGDIR.get().resolve("fhresearches/invalid-gametest.json");
        try {
            Files.writeString(invalid, "{\"category\":\"frostedresearch:rescue\",\"points\":0}");
            helper.assertTrue(!FHResearch.reloadCatalog(), "invalid hot reload must be rejected");
            helper.assertTrue(FHResearch.getResearch("root") == liveRoot,
                    "invalid hot reload must preserve the exact live catalogue");
        } catch (IOException e) {
            throw new IllegalStateException("Could not prepare invalid research catalogue", e);
        } finally {
            try {
                Files.deleteIfExists(invalid);
            } catch (IOException e) {
                throw new IllegalStateException("Could not clean invalid research catalogue", e);
            }
        }
    }

    private static ServerPlayer testPlayer(GameTestHelper helper) {
        return new ServerPlayer(
                helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "research-gametest"));
    }
}
