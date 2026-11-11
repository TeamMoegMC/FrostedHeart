/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.town;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
import com.teammoeg.frostedheart.content.town.buildings.institute.ResearchInstituteBlockEntity;
import com.teammoeg.frostedheart.content.town.buildings.institute.ResearchInstituteBuilding;
import com.teammoeg.frostedheart.content.town.buildings.institute.ResearchInstituteDailyModel;
import com.teammoeg.frostedheart.content.town.buildings.institute.TownResearchSupplies;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedresearch.FRConfig;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.blocks.DrawingDeskBlock;
import com.teammoeg.frostedresearch.knowledge.KnowledgeCapacity;
import com.teammoeg.frostedresearch.knowledge.KnowledgeService;
import com.teammoeg.frostedresearch.knowledge.ResearchResult;
import com.teammoeg.frostedresearch.knowledge.definition.IdeaDefinition;
import com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions;
import com.teammoeg.frostedresearch.knowledge.definition.ProjectDefinition;
import com.teammoeg.frostedresearch.knowledge.definition.ProjectRequirement;
import com.teammoeg.frostedresearch.knowledge.definition.ResultDefinition;
import com.teammoeg.frostedresearch.knowledge.labor.LaborReport;
import com.teammoeg.frostedresearch.knowledge.labor.ResearchLaborPool;
import com.teammoeg.frostedresearch.knowledge.link.LinkRule;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeElement;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import com.teammoeg.frostedresearch.knowledge.state.KnowledgeState;
import com.teammoeg.frostedresearch.knowledge.state.ResearchHistory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * 研究所：真实房间的书架、绘图台、装饰与照明；知识容量扩展；研究人力的分配、仓库物品与联想；存档往返。
 * <p>
 * Research institute: real-room furnishing and light, the knowledge capacity it grants, research labor allocation
 * with warehouse items and education levels, association, and save round trips. Tests that install knowledge
 * definitions run synchronously in their own batch and restore the previous definitions.
 */
@GameTestHolder("frostedheart_institute")
@PrefixGameTestTemplate(false)
public final class ResearchInstituteGameTests {
    private static final String NS = "institute_test";

    private ResearchInstituteGameTests() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(NS, path);
    }

    /** Two-block drawing desk with its main half at {@code main} and its other half to the east. */
    private static void placeDesk(GameTestHelper helper, BlockPos main) {
        var desk = FRContents.Blocks.DRAWING_DESK.get().defaultBlockState().setValue(DrawingDeskBlock.FACING, Direction.EAST);
        helper.getLevel().setBlock(main, desk, 2);
        helper.getLevel().setBlock(main.east(), desk.setValue(DrawingDeskBlock.IS_NOT_MAIN, true), 2);
    }

    /** An institute in a furnished 5x3x5 room: four bookshelves in the west wall, two desks, a lectern. */
    static ResearchInstituteBlockEntity furnishedInstitute(GameTestHelper helper, TeamDataHolder holder, BlockPos origin) {
        TownBuildingGameTests.buildEnclosedRoom(helper, origin);
        for (BlockPos shelf : List.of(origin.offset(0, 1, 1), origin.offset(0, 1, 2), origin.offset(0, 2, 1), origin.offset(0, 2, 2)))
            helper.getLevel().setBlockAndUpdate(shelf, Blocks.BOOKSHELF.defaultBlockState());
        placeDesk(helper, origin.offset(1, 1, 5));
        placeDesk(helper, origin.offset(4, 1, 5));
        helper.getLevel().setBlockAndUpdate(origin.offset(5, 1, 1), Blocks.LECTERN.defaultBlockState());
        return (ResearchInstituteBlockEntity) TownBuildingGameTests.placeCore(helper, holder, origin.offset(3, 0, 3),
                FHBlocks.RESEARCH_INSTITUTE.get());
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void roomCountsShelvesDesksDecorationAndNeedsLampLight(GameTestHelper helper) {
        TeamDataHolder holder = TownBuildingGameTests.newTeam(helper, "institute-room");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        ResearchInstituteBlockEntity core = furnishedInstitute(helper, holder, origin);
        ResearchInstituteBuilding institute = core.getBuilding().orElseThrow();
        core.refresh_safe(institute);
        helper.assertTrue(institute.isStructureValid(), "an enclosed room with a door is a valid institute");
        helper.assertTrue(institute.getBookshelves() == 4, "wall bookshelves count, got " + institute.getBookshelves());
        helper.assertTrue(institute.getDesks() == 2, "a two-block drawing desk is one desk, got " + institute.getDesks());
        helper.assertTrue(institute.getMaxResidents() == 2, "each desk seats one researcher, got " + institute.getMaxResidents());
        helper.assertTrue(institute.getDecorationRating() > 0, "a lectern decorates the room");
        helper.assertTrue(!institute.isLightValid() && !institute.isBuildingWorkable(),
                "an unlit room is too dark to work, average light " + institute.getAverageLight());
        int archiveBase = FRConfig.SERVER.knowledgeArchiveObservations.get();
        int inboxBase = FRConfig.SERVER.knowledgeInboxObservations.get();
        helper.assertTrue(KnowledgeCapacity.limits(holder).archive() == archiveBase, "an idle institute grants no capacity");

        for (int x = 1; x <= 5; x++)
            for (int z = 1; z <= 5; z++)
                helper.getLevel().setBlockAndUpdate(origin.offset(x, 4, z), Blocks.GLOWSTONE.defaultBlockState());
        // Block light propagates off-thread: rescan each tick until it arrives.
        helper.succeedWhen(() -> {
            core.refresh_safe(institute);
            helper.assertTrue(institute.isLightValid(), "a glowstone ceiling lights the room, got " + institute.getAverageLight());
            double minimum = FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE.minimumWorkingTemperatureCelsius.get();
            institute.setTemperature(minimum + 10 - institute.getTemperatureModifier());
            helper.assertTrue(institute.isBuildingWorkable(), "a lit, warm room works");
            int perArchive = FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE.archiveObservationsPerBookshelf.get();
            int perInbox = FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE.inboxObservationsPerBookshelf.get();
            var limits = KnowledgeCapacity.limits(holder);
            helper.assertTrue(limits.archive() == archiveBase + 4 * perArchive && limits.inbox() == inboxBase + 4 * perInbox,
                    "four bookshelves add capacity on top of the base, got " + limits);
            institute.setTemperature(minimum - 30 - institute.getTemperatureModifier());
            helper.assertTrue(!institute.isBuildingWorkable() && KnowledgeCapacity.limits(holder).archive() == archiveBase,
                    "a cold institute stops granting capacity");
        });
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void researchersGiveLaborByEducationAndStudy(GameTestHelper helper) {
        TeamDataHolder holder = TownBuildingGameTests.newTeam(helper, "institute-labor");
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        ResearchInstituteBlockEntity core = furnishedInstitute(helper, holder, origin);
        ResearchInstituteBuilding institute = core.getBuilding().orElseThrow();
        core.refresh_safe(institute);
        // Make the scanned room workable without waiting for light or heat.
        institute.setAverageLight(15);
        institute.setTemperature(40);
        helper.assertTrue(institute.isBuildingWorkable(), "the furnished room works once lit and warm");
        TeamTownData data = holder.getData(FHSpecialDataTypes.TOWN_DATA);
        TeamTown town = data.createTeamTown();
        Resident standard = researcher("Standard", 50, 1);
        Resident scholar = researcher("Scholar", 90, 3);
        for (Resident resident : List.of(standard, scholar)) {
            data.residents.put(resident.getUUID(), resident);
            institute.addResident(resident);
        }
        data.resources.resetAllServices();
        helper.assertTrue(institute.work(town, helper.getLevel()), "eligible researchers work");
        double multiplier = ResearchInstituteDailyModel.decorationMultiplier(institute.getDecorationRating(),
                FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE.decorationBonusAtFullRating.get());
        double primary = data.resources.get(VirtualResourceType.RESEARCH_LABOR.generateAttribute(1));
        double higher = data.resources.get(VirtualResourceType.RESEARCH_LABOR.generateAttribute(3));
        helper.assertTrue(Math.abs(primary - multiplier) < 1e-6,
                "a standard researcher gives one standard researcher of labor times decoration, got " + primary);
        helper.assertTrue(higher > primary, "higher intelligence gives more labor, got " + higher);
        helper.assertTrue(institute.getDailyReport().workerCount() == 2 && institute.getDailyReport().at(3) == higher,
                "the report keeps labor by education level");
        helper.assertTrue(standard.getDailyActivity().learning() == FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE.learningActivity.get(),
                "study records learning activity for intelligence growth");
        helper.assertTrue(TownResearchSettlement.laborPool(data.resources).available(3) == higher,
                "the settlement reads labor back by level");
        data.resources.resetAllServices();
        helper.assertTrue(data.resources.get(VirtualResourceType.RESEARCH_LABOR) == 0, "every labor level is a daily service");

        institute.setAverageLight(0);
        helper.assertTrue(!institute.work(town, helper.getLevel())
                        && institute.getDailyReport().stopReason() == TownProductionStopReason.BUILDING_UNWORKABLE,
                "a dark institute reports why it stopped");
        helper.succeed();
    }

    static Resident researcher(String name, double intelligence, int education) {
        Resident resident = new Resident(name, "Researcher", UUID.randomUUID());
        resident.setHealth(50);
        resident.setMental(50);
        resident.setStrength(50);
        resident.setIntelligence(intelligence);
        resident.setEducationLevel(education);
        resident.setHousePos(BlockPos.ZERO);
        return resident;
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 40)
    public static void instituteRecordRoundTripsThroughTheBuildingCodec(GameTestHelper helper) {
        ResearchInstituteBuilding institute = new ResearchInstituteBuilding(new BlockPos(1, 2, 3), true, true,
                com.teammoeg.frostedheart.content.town.block.OccupiedZoneInfo.EMPTY, List.of(UUID.randomUUID()), 20, 60, 2,
                12.5, 24, 9.5, 6, 2, 0.4,
                new ResearchInstituteBuilding.InstituteDailyReport(true, 1, List.of(0.0, 1.25), TownProductionStopReason.NONE));
        Tag encoded = ITownBuilding.CODEC.encodeStart(NbtOps.INSTANCE, institute).getOrThrow(false, message -> {});
        ResearchInstituteBuilding restored = (ResearchInstituteBuilding) ITownBuilding.CODEC.parse(NbtOps.INSTANCE, encoded)
                .getOrThrow(false, message -> {});
        helper.assertTrue(restored.getBookshelves() == 6 && restored.getDesks() == 2 && restored.getAverageLight() == 9.5
                        && restored.getDecorationRating() == 0.4 && restored.getTemperatureModifier() == 24
                        && restored.getMaxResidents() == 2 && restored.getResidentsID().size() == 1
                        && restored.getDailyReport().at(1) == 1.25,
                "every saved institute field survives the building codec");
        helper.succeed();
    }

    // ---- knowledge coupling ----------------------------------------------------------------------------------------

    private record Fixture(KnowledgeService service, KnowledgeState state, UUID run) {
    }

    private static final ResourceLocation ORIGIN = id("origin"), RESULT = id("result"), PROJECT = id("project");
    private static final ResourceLocation LEFT = id("left"), RIGHT = id("right"), INSIGHT = id("insight"), LINK = id("link");

    private static ProjectRequirement.Material paper(int count) {
        return new ProjectRequirement.Material(Optional.of(new ResourceLocation("minecraft", "paper")), Optional.empty(), count);
    }

    /** Installs a small definition set; the caller restores {@code previous}. */
    private static void install(List<ProjectRequirement> requirements) {
        var idea = new IdeaDefinition("origin", "", List.of(), List.of(PROJECT), true);
        var plain = new IdeaDefinition("plain", "", List.of(), List.of(), true);
        var result = new ResultDefinition(new ResearchResult.Finding(RESULT, List.of()), "result", "", List.of(), true);
        var link = new LinkRule(List.of(
                new LinkRule.Slot("a", KnowledgeKey.Kind.IDEA, Optional.of(LEFT), Optional.empty(), List.of()),
                new LinkRule.Slot("b", KnowledgeKey.Kind.IDEA, Optional.of(RIGHT), Optional.empty(), List.of())), List.of(), INSIGHT);
        KnowledgeDefinitions.install(new KnowledgeDefinitions.Snapshot(0,
                Map.of(ORIGIN, idea, LEFT, plain, RIGHT, plain, INSIGHT, plain), Map.of(RESULT, result), Map.of(LINK, link), Map.of(),
                Map.of(PROJECT, new ProjectDefinition("project", "", List.of(RESULT), requirements)), List.of()));
    }

    private static Fixture started(List<ProjectRequirement> requirements) {
        install(requirements);
        KnowledgeState state = new KnowledgeState();
        KnowledgeService service = new KnowledgeService(UUID.randomUUID(), state, 0, () -> {});
        var source = new AcquisitionSource("command", Optional.empty(), 0);
        for (ResourceLocation idea : List.of(ORIGIN, LEFT, RIGHT))
            service.learn(KnowledgeElement.idea(idea), source, KnowledgeService.Grant.COMMAND);
        UUID run = service.startResearch(PROJECT, KnowledgeKey.idea(ORIGIN)).orElseThrow();
        return new Fixture(service, state, run);
    }

    private static TownResearchSupplies warehouseWithPaper(GameTestHelper helper, String name, double paper) {
        TeamTown town = TownBuildingGameTests.newTeam(helper, name).getData(FHSpecialDataTypes.TOWN_DATA).createTeamTown();
        town.getActionExecutorHandler().execute(new TownResourceActions.VirtualResourceAttributeAction(
                VirtualResourceType.MAX_CAPACITY.generateAttribute(0), 1000, ResourceActionType.ADD, ResourceActionMode.ATTEMPT));
        town.getActionExecutorHandler().execute(new TownResourceActions.ItemResourceAction(
                new ItemStack(Items.PAPER), ResourceActionType.ADD, paper, ResourceActionMode.ATTEMPT));
        return new TownResearchSupplies(town);
    }

    @GameTest(template = "phase0a_empty", batch = "institute_knowledge", timeoutTicks = 40)
    public static void laborRequirementsValidateTheirShape(GameTestHelper helper) {
        var labor = new ProjectRequirement.Labor(true, 2, 3, 2, List.of(paper(1)));
        List<String> errors = new ArrayList<>();
        ProjectRequirement.calculation("pure", "", 0, 0).withLabor(labor).validate(errors);
        ProjectRequirement.experiment("staffed", "title", 0, 0, List.of(), ProjectRequirement.Space.NONE).withLabor(labor).validate(errors);
        ProjectRequirement.calculation("assist", "", 1, 2).withLabor(new ProjectRequirement.Labor(false, 1, 1, 0, List.of())).validate(errors);
        helper.assertTrue(errors.isEmpty(), "required labor may carry a requirement alone: " + errors);
        List<String> bad = new ArrayList<>();
        ProjectRequirement.calculation("optional_zero", "", 0, 0).withLabor(new ProjectRequirement.Labor(false, 1, 1, 0, List.of())).validate(bad);
        ProjectRequirement.calculation("workers", "", 0, 1).withLabor(new ProjectRequirement.Labor(false, 0, 1, 0, List.of())).validate(bad);
        ProjectRequirement.calculation("days", "", 0, 1).withLabor(new ProjectRequirement.Labor(false, 1, 0, 0, List.of())).validate(bad);
        ProjectRequirement.calculation("education", "", 0, 1).withLabor(new ProjectRequirement.Labor(false, 1, 1, 6, List.of())).validate(bad);
        ProjectRequirement.calculation("items", "", 0, 1).withLabor(new ProjectRequirement.Labor(false, 1, 1, 0,
                List.of(paper(1), paper(1), paper(1), paper(1), paper(1)))).validate(bad);
        ProjectRequirement.calculation("unknown", "", 0, 1).withLabor(new ProjectRequirement.Labor(false, 1, 1, 0,
                List.of(new ProjectRequirement.Material(Optional.of(new ResourceLocation(NS, "missing")), Optional.empty(), 1)))).validate(bad);
        ProjectRequirement.evidence("evidence", "title", List.of(new LinkRule.Slot("a", KnowledgeKey.Kind.OBSERVATION, Optional.empty(),
                Optional.empty(), List.of())), List.of(), List.of()).withLabor(labor).validate(bad);
        ProjectRequirement.experiment("timed", "title", 0, 400, List.of(), ProjectRequirement.Space.NONE).withLabor(labor).validate(bad);
        helper.assertTrue(bad.size() == 8, "each malformed labor is reported once, got " + bad);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "institute_knowledge", timeoutTicks = 40)
    public static void laborServesHarderWorkFirstAndDrawsItemsInProportion(GameTestHelper helper) {
        var previous = KnowledgeDefinitions.current();
        try {
            var assist = ProjectRequirement.calculation("assist", "", 0, 2)
                    .withLabor(new ProjectRequirement.Labor(false, 1, 1, 1, List.of(paper(2))));
            var staff = ProjectRequirement.calculation("staff", "", 0, 0)
                    .withLabor(new ProjectRequirement.Labor(true, 2, 2, 3, List.of(paper(1))));
            Fixture fixture = started(List.of(assist, staff));
            TownResearchSupplies supplies = warehouseWithPaper(helper, "institute-paper", 3);
            ResearchLaborPool pool = new ResearchLaborPool(List.of(0.0, 1.0, 0.0, 1.5));
            var allocations = fixture.service().workLabor(pool, supplies);
            helper.assertTrue(allocations.size() == 2 && allocations.get(0).requirement().equals("staff"),
                    "the requirement asking for more education is served first: " + allocations);
            helper.assertTrue(Math.abs(allocations.get(0).given() - 1.5) < 1e-9 && Math.abs(allocations.get(1).given() - 1.0) < 1e-9,
                    "each takes the labor it qualifies for: " + allocations);
            helper.assertTrue(pool.total() < 1e-9, "every unit of labor was used");
            helper.assertTrue(Math.abs(supplies.share(List.of(paper(1)), List.of(1.0)) - 0.25) < 1e-6,
                    "paper is drawn in proportion: 0.75 for 1.5 of 2 workers, 2 for one share");
            ResearchHistory run = fixture.state().research().get(fixture.run());
            helper.assertTrue(KnowledgeService.laborShares(run, assist) == 1 && KnowledgeService.playerRemaining(run, assist) == 1,
                    "one finished share stands in for one game");
            helper.assertTrue(fixture.service().calculationLevel(fixture.run(), "assist").isPresent()
                            && fixture.service().calculationLevel(fixture.run(), "staff").isEmpty(),
                    "the desk still takes the remaining game, and nothing for labor-only work");
            helper.assertTrue(!fixture.service().requirementMet(run, staff), "required labor is not finished at 1.5 of 4");

            ResearchLaborPool second = new ResearchLaborPool(List.of(0.0, 1.0, 0.0, 1.5));
            var shortDay = fixture.service().workLabor(second, supplies);
            helper.assertTrue(shortDay.get(0).itemsShort() && Math.abs(shortDay.get(0).given() - 0.5) < 1e-6,
                    "short paper scales the day's labor down: " + shortDay);
            helper.assertTrue(shortDay.get(1).itemsShort() && shortDay.get(1).given() == 0, "no paper, no labor: " + shortDay);
            fixture.service().recordCalculation(fixture.run(), "assist", 0);
            run = fixture.state().research().get(fixture.run());
            helper.assertTrue(fixture.service().requirementMet(run, assist), "the player's game and the share together meet the count");
            fixture.service().workLabor(new ResearchLaborPool(List.of(0.0, 0.0, 0.0, 9.0)), warehouseWithPaper(helper, "institute-paper-2", 50));
            fixture.service().workLabor(new ResearchLaborPool(List.of(0.0, 0.0, 0.0, 9.0)), warehouseWithPaper(helper, "institute-paper-3", 50));
            run = fixture.state().research().get(fixture.run());
            helper.assertTrue(fixture.service().requirementMet(run, staff) && fixture.service().requirementsMet(fixture.run()),
                    "the remaining labor finishes, at most two workers a day: " + run.labor());
            helper.assertTrue(KnowledgeService.laborDone(run, staff) <= 4 + 1e-9, "labor never exceeds what is still useful");
            helper.assertTrue(fixture.service().workLabor(new ResearchLaborPool(List.of(5.0, 5.0, 5.0, 5.0)), supplies).isEmpty(),
                    "met requirements ask for nothing");
        } finally {
            KnowledgeDefinitions.install(previous);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "institute_knowledge", timeoutTicks = 40)
    public static void associationLinksANewIdeaOnceAndSettlementKeepsTheReport(GameTestHelper helper) {
        var previous = KnowledgeDefinitions.current();
        try {
            Fixture fixture = started(List.of(ProjectRequirement.calculation("assist", "", 0, 1)
                    .withLabor(new ProjectRequirement.Labor(false, 1, 1, 0, List.of()))));
            var found = fixture.service().instituteAssociation(RandomSource.create(1), 1);
            helper.assertTrue(found.outcome() == LaborReport.Outcome.IDEA && found.subject().equals(Optional.of(KnowledgeKey.idea(INSIGHT))),
                    "researchers link two known ideas into the unknown one: " + found);
            helper.assertTrue(fixture.service().isActive(KnowledgeKey.idea(INSIGHT)) && fixture.state().links().containsKey(LINK),
                    "the idea is learned with its actual link");
            helper.assertTrue(fixture.state().acquisitions().stream().anyMatch(entry -> entry.source().mechanism().equals("institute:link:" + LINK)),
                    "the source names the institute");
            // The town day the idea was found on is kept, sent with the snapshot and read back by the journal.
            helper.assertTrue(fixture.state().lastDay(com.teammoeg.frostedresearch.knowledge.KnowledgeService.instituteFoundKey(KnowledgeKey.idea(INSIGHT))) == 1,
                    "the idea remembers the town day it was found on");
            var journal = new com.teammoeg.frostedresearch.knowledge.client.KnowledgeJournalData(
                    com.teammoeg.frostedresearch.knowledge.KnowledgeView.create(fixture.service()));
            helper.assertTrue(Long.valueOf(1).equals(journal.instituteDays.get(KnowledgeKey.idea(INSIGHT))),
                    "the journal snapshot carries that day: " + journal.instituteDays);
            helper.assertTrue(fixture.service().instituteAssociation(RandomSource.create(2), 2).outcome() == LaborReport.Outcome.NOTHING,
                    "a known idea is not found again");

            TownResearchSupplies supplies = warehouseWithPaper(helper, "institute-report", 0);
            LaborReport report = TownResearchSettlement.settle(new ResearchLaborPool(List.of(0.0, 0.0, 1000.0)), fixture.service(),
                    supplies, RandomSource.create(3), 7);
            helper.assertTrue(report.day() == 7 && report.allocations().size() == 1 && report.allocations().get(0).given() == 1.0,
                    "the settlement spends what the requirement asks: " + report);
            helper.assertTrue(report.association().chance() > 0.99 && report.association().outcome() == LaborReport.Outcome.NOTHING,
                    "idle educated labor associates: " + report.association());
            helper.assertTrue(fixture.state().laborReport().orElseThrow().equals(report), "the team keeps the day's report");
            helper.assertTrue(TownResearchSettlement.retireReport(fixture.service(), 8)
                            && fixture.state().laborReport().orElseThrow().day() == 8
                            && fixture.state().laborReport().orElseThrow().allocations().isEmpty(),
                    "a morning without labor replaces the old allocations");
            helper.assertTrue(!TownResearchSettlement.retireReport(fixture.service(), 9) && fixture.state().laborReport().orElseThrow().day() == 8,
                    "an empty report is replaced only once");
        } finally {
            KnowledgeDefinitions.install(previous);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "institute_knowledge", timeoutTicks = 40)
    public static void laborProgressAndReportSurviveSavingAndAbandoning(GameTestHelper helper) {
        ResearchHistory run = new ResearchHistory(UUID.randomUUID(), PROJECT, KnowledgeKey.idea(ORIGIN), 0, false, List.of())
                .withLabor("staff", 1.5).withLabor("staff", 0.25);
        Codec<ResearchHistory> codec = ResearchHistory.CODEC;
        ResearchHistory restored = codec.parse(NbtOps.INSTANCE, codec.encodeStart(NbtOps.INSTANCE, run).getOrThrow(false, m -> {}))
                .getOrThrow(false, m -> {});
        helper.assertTrue(restored.labor().get("staff") == 1.75, "labor worker-days accumulate and round-trip");
        helper.assertTrue(run.abandon().labor().isEmpty(), "abandoning drops labor with the other progress");
        KnowledgeState state = new KnowledgeState();
        LaborReport report = new LaborReport(3, List.of(1.0, 2.0), List.of(new LaborReport.Allocation(run.id(), "staff", 2, 1.5, true)),
                List.of(0.0, 0.5), new LaborReport.Association(LaborReport.Outcome.QUIET, 0.5, 0.07, Optional.empty()));
        state.laborReport(report);
        helper.assertTrue(KnowledgeState.read(state.write()).laborReport().orElseThrow().equals(report), "the report is saved with the team");
        var raw = state.write();
        raw.getCompound("labor_report").putString("day", "not a number");
        var damaged = KnowledgeState.read(raw);
        helper.assertTrue(damaged.laborReport().isEmpty() && damaged.write().contains("labor_report"),
                "an unreadable report stays out of play but is written back");
        helper.succeed();
    }
}
