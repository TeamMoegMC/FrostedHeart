/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import com.mojang.authlib.GameProfile;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.SinglePlayerTeamAPIProvider;
import com.teammoeg.chorda.dataholders.team.TeamDataClosure;
import com.teammoeg.chorda.dataholders.team.TeamsAPI;
import com.teammoeg.chorda.dataholders.team.TeamsAPIProvider;
import com.teammoeg.frostedresearch.api.ResearchDataAPI;
import com.teammoeg.frostedresearch.api.TeamResearchService;
import com.teammoeg.frostedresearch.blocks.MechCalcTileEntity;
import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.handler.ResearchCommonEvents;
import com.teammoeg.frostedresearch.item.UpgradePrototypeItem;
import com.teammoeg.frostedresearch.knowledge.PrototypeProfileDefinition;
import com.teammoeg.frostedresearch.knowledge.ResearchResult;
import com.teammoeg.frostedresearch.knowledge.ResearchResultCatalog;
import com.teammoeg.frostedresearch.knowledge.ResearchTopicDefinition;
import com.teammoeg.frostedresearch.knowledge.KnowledgeRecord;
import com.teammoeg.frostedresearch.knowledge.FieldComparisonArtifact;
import com.teammoeg.frostedresearch.knowledge.TechnologyAccessResolver;
import com.teammoeg.frostedheart.content.utility.oredetect.OreProspectingModel;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
            verifyPhaseOneResultAccess(helper, owner);
            verifyPhaseTwoRockAndOreLoop(helper, owner);
            verifyInvalidCatalogReloadPreservesLiveDefinitions(helper);
            verifyListenerAndEffectLifecycle(helper, owner, otherTeam);
            helper.succeed();
        } finally {
            TeamsAPI.register(previousProvider);
        }
    }

    private static void verifyPhaseTwoRockAndOreLoop(GameTestHelper helper, ServerPlayer player) {
        ResearchResultCatalog.Snapshot previous = ResearchResultCatalog.current();
        ResourceLocation recipeId = new ResourceLocation("minecraft", "stick");
        ResearchTopicDefinition topic = new ResearchTopicDefinition(3,
                ResearchTopicDefinition.Presentation.EMPTY,
                List.of(
                        new ResearchResult.Finding(TeamResearchService.ROCK_FINDING, List.of(
                                new ResourceLocation("frostedheart", "geology_archive"),
                                new ResourceLocation("frostedheart", "prospecting_report_detail"))),
                        new ResearchResult.Design(TeamResearchService.COPPER_PICK_DESIGN, List.of(recipeId))),
                List.of(), ResearchTopicDefinition.Legacy.NONE, List.of(), Optional.empty(),
                List.of(new ResearchTopicDefinition.Protocol(
                        new ResourceLocation("frostedheart", "compare_rock_samples"),
                        new ResourceLocation("frostedheart", "manual_field_comparison"),
                        List.of("match", "no_match", "insufficient"))),
                Optional.of(new ResearchTopicDefinition.Resolution(
                        new ResourceLocation("frostedheart", "field_comparison_resolution"),
                        TeamResearchService.ROCK_IDEA,
                        List.of(TeamResearchService.ROCK_FINDING, TeamResearchService.COPPER_PICK_DESIGN))));
        try {
            ResearchResultCatalog.install(new ResearchResultCatalog.Candidate(
                    Map.of(TeamResearchService.ROCK_TOPIC, topic), Map.of()));
            ResourceLocation dimension = helper.getLevel().dimension().location();
            ResourceLocation copper = new ResourceLocation("minecraft", "copper_ore");
            ResourceLocation stone = new ResourceLocation("minecraft", "stone");
            UUID observer = player.getUUID();
            KnowledgeRecord outcrop = KnowledgeRecord.create(KnowledgeRecord.Type.COPPER_OUTCROP,
                    dimension, new BlockPos(0, 2, 0), copper, 1, observer, Optional.empty());
            KnowledgeRecord nearby = KnowledgeRecord.create(KnowledgeRecord.Type.ROCK_SAMPLE,
                    dimension, new BlockPos(0, 2, 0), stone, 2, observer,
                    Optional.of(new OreProspectingModel.Snapshot(Map.of(copper, 3))));
            KnowledgeRecord control = KnowledgeRecord.create(KnowledgeRecord.Type.ROCK_SAMPLE,
                    dimension, new BlockPos(32, 2, 0), stone, 3, observer,
                    Optional.of(OreProspectingModel.Snapshot.EMPTY));
            helper.assertTrue(TeamResearchService.archiveObservation(player, outcrop),
                    "the field notebook path must archive the copper outcrop");
            helper.assertTrue(TeamResearchService.archiveObservation(player, nearby)
                            && TeamResearchService.archiveObservation(player, control),
                    "the field notebook path must archive nearby and control samples");
            helper.assertTrue(TeamResearchService.recordIdea(player, TeamResearchService.ROCK_TOPIC,
                            TeamResearchService.ROCK_IDEA, "gametest:inspiration",
                            Set.of(outcrop.id(), nearby.id())),
                    "completed V2 inspiration must record the candidate idea");
            helper.assertTrue(TeamResearchService.executeProtocolAction(player, TeamResearchService.ROCK_TOPIC,
                            new ResourceLocation("frostedheart", "compare_rock_samples")),
                    "one lightweight theory step must complete from the recorded ore and stone");
            helper.assertTrue(TeamResearchService.acceptTopicResults(player, TeamResearchService.ROCK_TOPIC),
                    "the completed elementary theory must resolve the topic without a control sample or MATCH gate");
            TeamKnowledgeData data = com.teammoeg.frostedresearch.api.KnowledgeDataAPI.getData(player).get();
            helper.assertTrue(data.hasFinding(TeamResearchService.ROCK_FINDING)
                            && data.hasDesign(TeamResearchService.COPPER_PICK_DESIGN),
                    "resolution must atomically acquire Finding and Design");
            var projection = TechnologyAccessResolver.projectKnowledge(data);
            helper.assertTrue(projection.observations().stream().anyMatch(summary ->
                            summary.id().equals(nearby.id())
                                    && summary.annotations().contains(
                                            com.teammoeg.frostedheart.content.utility.oredetect.GeologyResearchIntegration.TRACE_PRESENT))
                            && projection.observations().stream().anyMatch(summary ->
                            summary.id().equals(control.id())
                                    && summary.annotations().contains(
                                            com.teammoeg.frostedheart.content.utility.oredetect.GeologyResearchIntegration.TRACE_ABSENT)),
                    "Finding must reveal only coarse interpretable signs for rock samples");
            ResearchResultCatalog.install(new ResearchResultCatalog.Candidate(Map.of(), Map.of()));
            helper.assertTrue(data.idea(TeamResearchService.ROCK_TOPIC, TeamResearchService.ROCK_IDEA).isPresent()
                            && data.hasFinding(TeamResearchService.ROCK_FINDING),
                    "missing topic definitions must retain orphan history");
        } finally {
            ResearchResultCatalog.install(new ResearchResultCatalog.Candidate(
                    previous.topics(), previous.profiles()));
        }
    }

    private static void verifyPhaseOneResultAccess(GameTestHelper helper, ServerPlayer player) {
        ResearchResultCatalog.Snapshot previous = ResearchResultCatalog.current();
        ResourceLocation recipeId = new ResourceLocation("minecraft", "stick");
        ResourceLocation multiblockId = new ResourceLocation(
                "immersiveengineering", "multiblocks/blast_furnace");
        ResourceLocation blockId = FRContents.Blocks.MECHANICAL_CALCULATOR.getId();
        ResourceLocation topicId = new ResourceLocation(FRMain.MODID, "gametest/results");
        ResourceLocation findingId = new ResourceLocation(FRMain.MODID, "gametest/finding");
        ResourceLocation designId = new ResourceLocation(FRMain.MODID, "gametest/design");
        ResourceLocation constructionId = new ResourceLocation(FRMain.MODID, "gametest/construction");
        ResourceLocation procedureId = new ResourceLocation(FRMain.MODID, "gametest/procedure");
        ResourceLocation prototypeId = new ResourceLocation(FRMain.MODID, "gametest/prototype");
        ResourceLocation profileId = new ResourceLocation(FRMain.MODID, "gametest/profile");

        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(recipeId).orElse(null);
        IMultiblock multiblock = MultiblockHandler.getByUniqueName(multiblockId);
        Block usableBlock = FRContents.Blocks.MECHANICAL_CALCULATOR.get();
        helper.assertTrue(recipe != null, "the real vanilla stick recipe must exist");
        helper.assertTrue(multiblock != null, "the real IE blast furnace multiblock must exist");

        ResearchTopicDefinition topic = new ResearchTopicDefinition(3,
                ResearchTopicDefinition.Presentation.EMPTY,
                List.of(
                        new ResearchResult.Finding(findingId, List.of()),
                        new ResearchResult.Design(designId, List.of(recipeId)),
                        new ResearchResult.Construction(constructionId, List.of(multiblockId)),
                        new ResearchResult.Procedure(procedureId, List.of(blockId)),
                        new ResearchResult.Prototype(prototypeId, profileId)),
                List.of());
        try {
            ResearchResultCatalog.install(new ResearchResultCatalog.Candidate(
                    Map.of(topicId, topic), Map.of(profileId, new PrototypeProfileDefinition(1, 7))));

            helper.assertTrue(!ResearchHooks.canUseRecipe(player, recipe),
                    "an acquired-source-free managed recipe must be locked");
            helper.assertTrue(!ResearchHooks.canFormMultiblock(player, multiblock),
                    "an acquired-source-free managed multiblock must be locked");
            PlayerInteractEvent.RightClickBlock lockedUse = rightClick(player, helper, usableBlock);
            ResearchCommonEvents.canUseBlock(lockedUse);
            helper.assertTrue(lockedUse.getUseBlock() == net.minecraftforge.eventbus.api.Event.Result.DENY,
                    "the real RightClickBlock handler must deny an acquired-source-free managed block");

            helper.assertTrue(TeamResearchService.grantResult(player, findingId).status()
                            == TeamResearchService.Status.ACQUIRED,
                    "a Finding must be acquired through TeamResearchService");
            UUID teamId = com.teammoeg.frostedresearch.api.KnowledgeDataAPI.getData(player).team().getId();
            helper.assertTrue(TechnologyAccessResolver.hasFinding(teamId, findingId),
                    "the acquired Finding must enter KnowledgeProjection");
            helper.assertTrue(!ResearchHooks.canUseRecipe(player, recipe)
                            && !ResearchHooks.canFormMultiblock(player, multiblock),
                    "a Finding must not change technology access");

            helper.assertTrue(TeamResearchService.grantResult(player, designId).status()
                            == TeamResearchService.Status.ACQUIRED,
                    "a Design must be acquired through TeamResearchService");
            helper.assertTrue(ResearchHooks.canUseRecipe(player, recipe)
                            && !ResearchHooks.canFormMultiblock(player, multiblock),
                    "a Design must unlock only its real recipe channel");
            helper.assertTrue(TeamResearchService.grantResult(player, designId).status()
                            == TeamResearchService.Status.ALREADY_ACQUIRED,
                    "a repeated Design grant must be idempotent");

            helper.assertTrue(TeamResearchService.grantResult(player, constructionId).status()
                            == TeamResearchService.Status.ACQUIRED,
                    "a Construction must be acquired through TeamResearchService");
            helper.assertTrue(ResearchHooks.canFormMultiblock(player, multiblock),
                    "a Construction must unlock the real IE formation channel");

            helper.assertTrue(TeamResearchService.grantResult(player, procedureId).status()
                            == TeamResearchService.Status.ACQUIRED,
                    "a Procedure must be acquired through TeamResearchService");
            PlayerInteractEvent.RightClickBlock allowedUse = rightClick(player, helper, usableBlock);
            ResearchCommonEvents.canUseBlock(allowedUse);
            helper.assertTrue(allowedUse.getUseBlock() != net.minecraftforge.eventbus.api.Event.Result.DENY,
                    "the acquired Procedure must pass the real RightClickBlock handler");

            fillInventory(player);
            int nearbyBefore = nearbyPrototypeDrops(helper, player).size();
            TeamResearchService.GrantResult firstPrototype = TeamResearchService.grantResult(player, prototypeId);
            TeamResearchService.GrantResult secondPrototype = TeamResearchService.grantResult(player, prototypeId);
            helper.assertTrue(firstPrototype.status() == TeamResearchService.Status.FABRICATED
                            && secondPrototype.status() == TeamResearchService.Status.FABRICATED,
                    "a Prototype result must fabricate on every grant");
            helper.assertTrue(firstPrototype.prototype() != null && secondPrototype.prototype() != null
                            && !firstPrototype.prototype().serial().equals(secondPrototype.prototype().serial()),
                    "fabricated prototypes must have distinct serials");
            helper.assertTrue(firstPrototype.prototype().profile().equals(profileId)
                            && firstPrototype.prototype().profileRevision() == 7
                            && firstPrototype.prototype().ownerTeam().equals(teamId),
                    "fabrication must freeze profile, revision, and owner team");
            helper.assertTrue(nearbyPrototypeDrops(helper, player).size() >= nearbyBefore + 2,
                    "full-inventory prototype delivery must drop both physical items nearby");

            TeamKnowledgeData data = com.teammoeg.frostedresearch.api.KnowledgeDataAPI.getData(player).get();
            helper.assertTrue(data.findingIds().contains(findingId)
                            && data.designIds().contains(designId)
                            && data.constructionIds().contains(constructionId)
                            && data.procedureIds().contains(procedureId),
                    "team authority must contain exactly the four acquirable result categories");
        } finally {
            ResearchResultCatalog.install(new ResearchResultCatalog.Candidate(
                    previous.topics(), previous.profiles()));
            player.getInventory().clearContent();
        }
    }

    private static PlayerInteractEvent.RightClickBlock rightClick(
            ServerPlayer player, GameTestHelper helper, Block block) {
        BlockPos pos = new BlockPos(2, 1, 1);
        helper.setBlock(pos, block);
        BlockPos absolutePos = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolutePos),
                net.minecraft.core.Direction.UP, absolutePos, false);
        return new PlayerInteractEvent.RightClickBlock(
                player, InteractionHand.MAIN_HAND, absolutePos, hit);
    }

    private static void fillInventory(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            player.getInventory().items.set(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
    }

    private static List<ItemEntity> nearbyPrototypeDrops(GameTestHelper helper, ServerPlayer player) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(player.blockPosition()).inflate(4), entity ->
                        entity.getItem().getItem() instanceof UpgradePrototypeItem);
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
            Recipe<?> legacyRecipe = helper.getLevel().getRecipeManager()
                    .byKey(new ResourceLocation("minecraft", "stick")).orElseThrow();
            IMultiblock legacyMultiblock = MultiblockHandler.getByUniqueName(new ResourceLocation(
                    "immersiveengineering", "multiblocks/blast_furnace"));
            var legacyProjection = TechnologyAccessResolver.project(ownerData.team());
            helper.assertTrue(legacyProjection.recipe(legacyRecipe.getId()).allowed()
                            && legacyProjection.recipe(legacyRecipe.getId()).sources().stream()
                                    .allMatch(com.teammoeg.frostedresearch.knowledge.AccessSource.LegacySource.class::isInstance),
                    "EffectCrafting must project only as a legacy recipe entitlement");
            helper.assertTrue(legacyProjection.multiblock(legacyMultiblock.getUniqueName()).allowed()
                            && legacyProjection.multiblock(legacyMultiblock.getUniqueName()).sources().stream()
                                    .allMatch(com.teammoeg.frostedresearch.knowledge.AccessSource.LegacySource.class::isInstance),
                    "EffectBuilding must project only as a legacy Construction entitlement");
            helper.assertTrue(legacyProjection.block(FRContents.Blocks.MECHANICAL_CALCULATOR.getId()).allowed()
                            && legacyProjection.block(FRContents.Blocks.MECHANICAL_CALCULATOR.getId()).sources().stream()
                                    .allMatch(com.teammoeg.frostedresearch.knowledge.AccessSource.LegacySource.class::isInstance),
                    "EffectUse must project only as a legacy Procedure entitlement");
            helper.assertTrue(ownerData.get().getCurrentResearchValue() == listeners,
                    "completing a non-current research must preserve the current selection");
            ownerData.get().resetData(ownerData.team(), effectReset);
            helper.assertTrue(ownerData.get().getVariants().getDouble("gametest_stat") == 0.0D
                            && !ownerData.get().getData(effectReset).isCompleted(),
                    "research reset must revoke the reversible effect");
            helper.assertTrue(!ResearchHooks.canUseRecipe(owner, legacyRecipe)
                            && !ResearchHooks.canFormMultiblock(owner, legacyMultiblock)
                            && !ResearchHooks.canUseBlock(owner, FRContents.Blocks.MECHANICAL_CALCULATOR.get()),
                    "legacy reset must remove all three matching entitlement sources");

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
                    {"type":"stats","id":"stats","vars":"gametest_stat","val":5.0,"percent":false},
                    {"type":"recipe","id":"recipe","recipes":["minecraft:stick"]},
                    {"type":"multiblock","id":"construction","multiblock":"immersiveengineering:multiblocks/blast_furnace"},
                    {"type":"use","id":"procedure","blocks":["frostedresearch:mechanical_calculator"]}
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
